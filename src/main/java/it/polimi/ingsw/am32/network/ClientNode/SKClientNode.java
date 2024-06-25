package it.polimi.ingsw.am32.network.ClientNode;

import it.polimi.ingsw.am32.client.View;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSLobbyMessage;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSMessage;
import it.polimi.ingsw.am32.message.ClientToServer.PingMessage;
import it.polimi.ingsw.am32.message.ServerToClient.PongMessage;
import it.polimi.ingsw.am32.message.ServerToClient.StoCMessage;
import it.polimi.ingsw.am32.network.exceptions.ConnectionSetupFailedException;
import it.polimi.ingsw.am32.network.exceptions.NodeClosedException;
import it.polimi.ingsw.am32.network.exceptions.UploadFailureException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SKClientNode implements ClientNodeInterface, Runnable {

    private static final int PONGMAXCOUNT = 3;
    private static final int SOCKETTIMEOUT = 100;
    private static final int PINGINTERVAL = 5000;

    private final Logger logger;
    private final ExecutorService executorService;

    private final View view;
    private final String ip;
    private final int port;
    private final String nickname;
    private int pongCount;

    private Socket socket;
    private ObjectOutputStream outputObtStr;
    private ObjectInputStream inputObtStr;

    private ClientPingTask clientPingTask;
    private final Timer timer;

    private boolean statusIsAlive;
    private boolean reconnectCalled;
    private final Object aliveLock;
    private final Object cToSProcessingLock;
    private final Object sToCProcessingLock;

    public SKClientNode(View view, String ip, int port) throws ConnectionSetupFailedException {
        this.view = view;
        this.ip = ip;
        this.port = port;
        statusIsAlive = true;
        reconnectCalled = false;
        pongCount = PONGMAXCOUNT; // todo fare un config??
        nickname = "Unknown";

        logger = LogManager.getLogger(SKClientNode.class);

        try {

            logger.info("Attempting to connect to the server at {}:{}", ip, port);

            socket = new Socket(ip, port);
            outputObtStr = new ObjectOutputStream(socket.getOutputStream());
            outputObtStr.flush();
            inputObtStr = new ObjectInputStream(socket.getInputStream());
            socket.setSoTimeout(SOCKETTIMEOUT);

            logger.info("Connection established. Personal connection data: {}", socket.getLocalSocketAddress());

        } catch (IOException e) {

            if (inputObtStr != null) {
                try {
                    inputObtStr.close();
                } catch (IOException ignore) {}
            }

            if (outputObtStr != null) {
                try {
                    outputObtStr.close();
                } catch (IOException ignore) {}
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignore) {}
            }

            //System.out.println("Connection failed do to wrong parameters or inaccessible server");
            logger.info("Connection failed do to wrong parameters or inaccessible server");

            throw new ConnectionSetupFailedException();
        }

        executorService = Executors.newCachedThreadPool();
        clientPingTask = new ClientPingTask(this);
        timer = new Timer();
        aliveLock = new Object();
        cToSProcessingLock = new Object();
        sToCProcessingLock = new Object();
        timer.scheduleAtFixedRate(clientPingTask, PINGINTERVAL, PINGINTERVAL);
    }

    public void run() {

        // Listen for incoming messages
        while(true) {
            try {

                checkConnection();

                listenForIncomingMessages();

            } catch (IOException | ClassNotFoundException | NodeClosedException e) {
                logger.error("inputObtStr exception: {}. {}. {}",e.getClass(), e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
                resetConnection();
            }
        }
    }

    public void listenForIncomingMessages() throws IOException, ClassNotFoundException, NodeClosedException {

        Object message;

        try {
            synchronized (sToCProcessingLock) {
                message = inputObtStr.readObject();
            }
        } catch (SocketTimeoutException e) {
            // logger.debug("Socket timeout exception"); Disabled because it's too verbose
            return;
        }

        // TODO server sync??

        resetTimeCounter();


        if(message instanceof PongMessage) {

            logger.debug("PongMessage received");
            return;
        }

        if(message instanceof StoCMessage) {

            try {
                logger.info("Message received. Type: StoCMessage. Processing: {}", message);
                ((StoCMessage) message).processMessage(view);
            } catch (Exception e) {
                logger.fatal("Critical Runtime Exception:\nException Type: {}\nLocal Message: {}\nStackTrace: {}",
                        e.getClass(), e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
            }

        } else {

            logger.error("Message received. Message type not recognized");
        }
    }

    @Override
    public void uploadToServer(CtoSLobbyMessage message) throws UploadFailureException {

        try {
            synchronized (cToSProcessingLock) {
                outputObtStr.writeObject(message);

                try {
                    outputObtStr.flush();
                } catch (IOException ignore) {}
            }

            logger.info("Message sent. Type: CtoSLobbyMessage. Content: {}", message);

        } catch (IOException | NullPointerException e) {

            logger.info("Failed to send CtoSLobbyMessage to server. Exception: {}", e.getMessage());

            executorService.submit(this::resetConnection);

            throw new UploadFailureException();
        }
    }

    @Override
    public void uploadToServer(CtoSMessage message) throws UploadFailureException {

        try {
            synchronized (cToSProcessingLock) {
                outputObtStr.writeObject(message);

                try {
                    outputObtStr.flush();
                } catch (IOException ignore) {}
            }

            logger.info("Message sent. Type: CtoSMessage: {}", message);

        } catch (IOException | NullPointerException e) {

            logger.info("Failed to send CtoSMessage to server. Exception: {}", e.getMessage());

            executorService.submit(this::resetConnection);

            throw new UploadFailureException();
        }
    }

    private void checkConnection() {

        boolean tmpReconnect;

        synchronized (aliveLock) {

            if (statusIsAlive) {
                return;
            }

            tmpReconnect = manageReconnectionRequests();
        }

        if(tmpReconnect) {
            logger.info("Connection status: Down. Reconnecting...");
            connect();
        }
    }

    private void resetConnection() {

        boolean tmpReconnect;

        synchronized (aliveLock) {

            statusIsAlive = false;

            tmpReconnect =  manageReconnectionRequests();
        }

        if(tmpReconnect) {
            logger.info("Connection status: Down. Reconnecting...");
            connect();
        }
    }

    private boolean manageReconnectionRequests() {

        if(reconnectCalled){

            try {
                aliveLock.wait();
            } catch (InterruptedException | IllegalMonitorStateException ignore) {
            }

            return false;

        } else {
            reconnectCalled = true;
            clientPingTask.cancel();
            timer.purge();
            clientPingTask = new ClientPingTask(this);
            view.nodeDisconnected();
            return true;
        }
    }

    private void connect() {

        synchronized (sToCProcessingLock) {
            synchronized (cToSProcessingLock) {

                boolean reconnectionProcess = true;

                while (reconnectionProcess) {

                    if (inputObtStr != null) {
                        try {
                            inputObtStr.close();
                        } catch (IOException ignore) {}
                    }

                    if (outputObtStr != null) {
                        try {
                            outputObtStr.close();
                        } catch (IOException ignore) {}
                    }

                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ignore) {}
                    }

                    try {
                        socket = new Socket(ip, port);
                        outputObtStr = new ObjectOutputStream(socket.getOutputStream());
                        outputObtStr.flush();
                        inputObtStr = new ObjectInputStream(socket.getInputStream());
                        socket.setSoTimeout(SOCKETTIMEOUT);
                        logger.info("Connection established. Personal connection data: {}", socket.getLocalSocketAddress());

                    } catch (IOException ignore) {

                        logger.debug("Failed to connect to {}:{}", ip, port);
                        try {
                            Thread.sleep(100); // TODO parametrizzazione con config?
                        } catch (InterruptedException ignore2) {}

                        continue;
                    }

                    reconnectionProcess = false;

                }
            }
        }

        synchronized (aliveLock) {
            statusIsAlive = true;
            reconnectCalled = false;
            pongCount = PONGMAXCOUNT;
            aliveLock.notifyAll();
            timer.scheduleAtFixedRate(clientPingTask, PINGINTERVAL, PINGINTERVAL);
            view.nodeReconnected();
        }
    }

    public void startConnection(){

        executorService.submit(this);
        logger.debug("SKClientNode started");
    }

    @Override
    public void pongTimeOverdue() {

        boolean toReset = false;

        synchronized (aliveLock){
            if(!statusIsAlive)
                return;

            pongCount--;

            logger.debug("Pong time overdue. Pong count: {}", pongCount);

            if(pongCount <= 0) {
                logger.info("Pong count reached minimum. Trying to check connection");
                toReset = true;
            }
        }

        if(toReset){
            resetConnection();
            return;
        }

        executorService.submit(() -> {synchronized (cToSProcessingLock) {
            try {
                outputObtStr.writeObject(new PingMessage(nickname));
            } catch (IOException | NullPointerException ignore) {}
        }});
    }

    public void resetTimeCounter() {

        synchronized (aliveLock) {

            if (!statusIsAlive)
                return;

            pongCount = PONGMAXCOUNT; // TODO modificare se si aggiunge config
        }

        logger.debug("Pong count reset");
    }
}