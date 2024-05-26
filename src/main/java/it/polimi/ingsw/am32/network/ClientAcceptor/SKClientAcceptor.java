package it.polimi.ingsw.am32.network.ClientAcceptor;

import it.polimi.ingsw.am32.Utilities.Configuration;
import it.polimi.ingsw.am32.network.ServerNode.SKServerNode;
import it.polimi.ingsw.am32.network.exceptions.UninitializedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class SKClientAcceptor implements Runnable {

    private static final Logger logger = LogManager.getLogger(SKClientAcceptor.class);

    public void run() {

        ExecutorService executorService = Configuration.getInstance().getExecutorService();
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(Configuration.getInstance().getSocketPort());
        } catch (IOException e) {
            logger.fatal("Socket communications not available. ServerSocket initialization failed");
            return;
        }

        logger.debug("Server Socket Thread initialized successfully");

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                executorService.submit(new SKServerNode(socket));
                logger.info("Accepted connection from: {}. SKServerNode created successfully", socket.getRemoteSocketAddress());

            } catch (IOException e) {
                logger.error("Connection accept failed: {}", e.getMessage());
            } catch (UninitializedException e) {
                logger.error("SKServerNode initialization failed");
            }
        }
    }
}
