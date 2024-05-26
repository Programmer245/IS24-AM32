package it.polimi.ingsw.am32.network.ClientNode;

import it.polimi.ingsw.am32.client.View;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSLobbyMessage;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSMessage;
import it.polimi.ingsw.am32.message.ClientToServer.PingMessage;
import it.polimi.ingsw.am32.message.ServerToClient.StoCMessage;
import it.polimi.ingsw.am32.network.exceptions.NodeClosedException;
import it.polimi.ingsw.am32.network.exceptions.UploadFailureException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;

public class SKClientNode implements ClientNodeInterface, Runnable {

    private final Logger logger;
    private View view;
    private Socket socket;
    private String ip;
    private int port;
    private ObjectOutputStream outputObtStr;
    private ObjectInputStream inputObtStr;
    private String nickname;
    private ClientPingTask clientPingTask;
    private Timer timer;
    private int pongCount;
    private boolean statusIsAlive;
    private boolean reconnectCalled;
    private final Object aliveLock;
    private final Object ctoSProcessingLock;
    private final Object stoCProcessingLock;

    public SKClientNode(View view, String ip, int port) {
        this.view = view;
        this.ip = ip;
        this.port = port;
        clientPingTask = new ClientPingTask(this);
        timer = new Timer();
        aliveLock = new Object();
        ctoSProcessingLock = new Object();
        stoCProcessingLock = new Object();
        statusIsAlive = false;
        pongCount = 3; // todo fare un config??
        logger = LogManager.getLogger("SKClientNode");
        reconnectCalled = false;
    }

    public void run() {

        // Listen for incoming messages
        while(true) {
            try {

                checkConnection();

                listenForIncomingMessages();

            } catch (IOException | ClassNotFoundException | NodeClosedException ignore) {}
            //TODO forse il catch deve fare qualcosa
        }
    }

    public void listenForIncomingMessages() throws IOException, ClassNotFoundException, NodeClosedException {

        Object message;

        try {
            message = inputObtStr.readObject();
        } catch (SocketTimeoutException e) {return;}

        // TODO server sync??
        resetTimeCounter();

        if(message instanceof StoCMessage) {

            ((StoCMessage) message).processMessage(view);
            logger.info("Message received. Type: StoCMessage. Processing");

        } else {

            logger.info("Message received. Message type not recognized");
        }
    }

    @Override
    public void uploadToServer(CtoSLobbyMessage message) {

        boolean toSend = true;

        while (toSend) {
            try {
                synchronized (ctoSProcessingLock) {
                    outputObtStr.writeObject(message);
                }
                logger.info("Message sent. Type: CtoSLobbyMessage");
                toSend = false;
            } catch (IOException e) {
                checkConnection();
            }
        }
    }

    @Override
    public void uploadToServer(CtoSMessage message) {

        boolean toSend = true;

        while (toSend) {
            try {
                synchronized (ctoSProcessingLock) {
                    outputObtStr.writeObject(message);
                }
                logger.info("Message sent. Type: CtoSMessage");
                toSend = false;
            } catch (IOException e) {
                checkConnection();
            }
        }
    }

    private void checkConnection() {

        boolean tmpReconnect = false;

        synchronized (aliveLock) {
            if (statusIsAlive) {
                logger.info("Connection status: Alive");
                return;
            }

            if(reconnectCalled){
                try {
                    aliveLock.wait();
                } catch (InterruptedException ignore) {}
            } else {
                reconnectCalled = true;
                clientPingTask.cancel();
                timer.purge();
                clientPingTask = new ClientPingTask(this);
                tmpReconnect = true;
            }
        }

        if(tmpReconnect) {
            logger.info("Connection status: Down. Reconnecting...");
            connect();
        }

    }

    private void connect() {

        boolean reconnectionProcess = true;

        while (reconnectionProcess) {

            if (socket != null && !socket.isClosed()) {

                try {
                    inputObtStr.close();
                } catch (IOException ignore) {}

                try {
                    outputObtStr.close();
                } catch (IOException ignore) {}

                try {
                    socket.close();
                } catch (IOException ignore) {}
            }

            try {
                socket = new Socket(ip, port);
                inputObtStr = new ObjectInputStream(socket.getInputStream());
                outputObtStr = new ObjectOutputStream(socket.getOutputStream());

            } catch (IOException ignore) {

                logger.info("Failed to connect to {}:{}", ip, port);
                try {
                    wait(100); // TODO parametrizzazione con config?
                } catch (InterruptedException ignore2) {}

                continue;
            }

            reconnectionProcess = false;

        }

        logger.info("Connection established");

        synchronized (aliveLock) {
            statusIsAlive = true;
            reconnectCalled = false;
            aliveLock.notifyAll();
            timer.scheduleAtFixedRate(clientPingTask, 0, 5000);
        }
    }

    public void startConnection(){

        connect();
        // TODO da fare/controllare
        new Thread(this).start();
        logger.info("SKClientNode started");
    }

    @Override
    public void pongTimeOverdue() {
        try {

            // TODO Fare
            logger.info("Pong time overdue. Pong count: {}", pongCount);
            synchronized (ctoSProcessingLock) {
                outputObtStr.writeObject(new PingMessage(nickname));
            }
        } catch (IOException ignored) {}
    }

    public void resetTimeCounter() {

        synchronized (aliveLock) {

            if (!statusIsAlive)
                return;

            pongCount = 3; // TODO modificare se si aggiunge config
        }

        logger.info("Pong count reset");
    }
}