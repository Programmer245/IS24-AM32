package it.polimi.ingsw.am32.utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am32.Server;
import it.polimi.ingsw.am32.network.ServerNode.ServerPingTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is responsible for managing and storing server configuration parameters.
 * <br>
 * This class implements the <i><b>singleton pattern</b></i> allowing the server configuration to be unique and
 * retrievable everywhere in the project.
 * <br>
 * Configuration options come from 3 sources: hardcoded, config file, startup parameters
 *
 * @author Matteo
 */
public class Configuration {

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    //---------------------------------------------------------------------------------------------
    // Variables and Constants

    private static final String JSON_CONFIG_FILE_NAME = "Config.json";
    private static Configuration instance;
    private int rmiPort;
    private int socketPort;
    private int pingTimeInterval;
    private int maxPingCount;
    private int socketReadTimeout;
    private int endGameDueToDisconnectionTimeout;
    private String serverIp;
    private final ExecutorService executorService;
    private final Timer notLinkedSocketTimer;


    //---------------------------------------------------------------------------------------------
    // Constructor

    /**
     * This is a private constructor according to <i><b>singleton pattern</b></i> characteristics.
     * <br>
     * The server configurations are by default hardcoded but can be overwritten by a config file or through the
     * parameters given.
     * <br>
     * Overwrite rules are as follows:
     * <li>The overwrite priority is, in order: startup parameters (most important), config file, hardcoded (least important)</li><br>
     * <li>Socket port number is prioritized over RMI port number</li>
     *
     * @param args is an array of strings used as configuration parameters
     */
    private Configuration(String[] args) {

        // standard config values

        socketPort = 30000;
        rmiPort = 30001;
        pingTimeInterval = 5000;
        maxPingCount = 3;
        socketReadTimeout = 100;
        serverIp = "127.0.0.1";
        executorService = Executors.newCachedThreadPool();
        notLinkedSocketTimer = new Timer();
        endGameDueToDisconnectionTimeout = 2 * 60 * 1000; // 2 minutes

        // temporary values

        int rmiPortFile = -1;
        int rmiPortArgs = -2;
        int socketPortFile = -3;
        int socketPortArgs = -4;

        // overwrite server parameters with data from config file

        JsonNode jsonNode = null;
        boolean valid;

        try {
            // The configuration file will be read and parsed assuming the file is in the same directory as the JAR.
            // Therefore, we are getting the JAVA working directory and searching the file there.
            logger.debug("Attempting to load configuration file: {}", Paths.get(System.getProperty("user.dir"), JSON_CONFIG_FILE_NAME));
            String configFileContent = new String(
                    Files.readAllBytes(Paths.get(System.getProperty("user.dir"), JSON_CONFIG_FILE_NAME))
            );

            ObjectMapper objectMapper = new ObjectMapper();

            jsonNode = objectMapper.readTree(configFileContent);

            valid = true;

        } catch (IOException e) {
            valid = false;
        }

        if(valid){
            try {
                socketPortFile = portValidator(jsonNode.get("socketPort").asInt(), socketPortFile);
            } catch (Exception ignored){}

            try {
                rmiPortFile = portValidator(jsonNode.get("rmiPort").asInt(), rmiPortFile);
            } catch (Exception ignored){}

            try {
                pingTimeInterval = jsonNode.get("pingTimePeriod").asInt();
            } catch (Exception ignored){}

            try {
                maxPingCount = jsonNode.get("maxPingCount").asInt();
            } catch (Exception ignored){}

            try {
                socketReadTimeout = jsonNode.get("socketReadTimeout").asInt();
            } catch (Exception ignored){}

            try {
                serverIp = serverIpValidator(jsonNode.get("serverIp").asText(), serverIp);
            } catch (Exception ignored){}

            try {
                endGameDueToDisconnectionTimeout = jsonNode.get("endGameDueToDisconnectionTimeout").asInt();
            } catch (Exception ignored){}
        }

        // overwrite server configuration with data from startup parameters

        int i = 0;
        while (i < args.length) {

            if(i+1 >= args.length) {
                i++;
                continue;
            }

            if(!args[i].startsWith("-") || args[i+1].startsWith("-")) {
                i++;
                continue;
            }

            String arg = args[i].toLowerCase();

            try {
                switch (arg) {
                    case "-sp" -> socketPortArgs = portValidator(Integer.parseInt(args[i + 1]), socketPortArgs);
                    case "-rp" -> rmiPortArgs = portValidator(Integer.parseInt(args[i + 1]), rmiPortArgs);
                    case "-ptp" -> pingTimeInterval = Integer.parseInt(args[i + 1]);
                    case "-mpc" -> maxPingCount = Integer.parseInt(args[i + 1]);
                    case "-srt" -> socketReadTimeout = Integer.parseInt(args[i + 1]);
                    case "-edt" -> endGameDueToDisconnectionTimeout = Integer.parseInt(args[i + 1]);
                    case "-si" -> serverIp = serverIpValidator(args[i + 1], serverIp);
                }
            } catch (NumberFormatException ignored) {}

            i += 2;
        }

        // port incompatibility resolution

        if(socketPortArgs > 0) {
            socketPort = socketPortArgs;
            if (rmiPortArgs > 0 && rmiPortArgs != socketPortArgs)
                rmiPort = rmiPortArgs;
            else if(rmiPortFile > 0 && rmiPortFile != socketPortArgs)
                rmiPort = rmiPortFile;
        } else if(rmiPortArgs > 0) {
            rmiPort = rmiPortArgs;
            if(socketPortFile > 0 && socketPortFile != rmiPortArgs)
                socketPort = socketPortFile;
        } else {
            if(socketPortFile > 0)
                socketPort = socketPortFile;
            if(rmiPortFile > 0 && rmiPortFile != socketPortFile)
                rmiPort = rmiPortFile;
        }

        logger.info("The loaded configuration is:");
        logger.info("Socket port: {}", socketPort);
        logger.info("RMI port: {}", rmiPort);
        logger.info("Ping time period: {}", pingTimeInterval);
        logger.info("Max ping count: {}", maxPingCount);
        logger.info("Socket read timeout: {}", socketReadTimeout);
        logger.info("End game due to disconnection timeout: {}", endGameDueToDisconnectionTimeout);
        logger.info("Server IP: {}", serverIp);
    }


    //---------------------------------------------------------------------------------------------
    // Methods

    /**
     * Verify that the new port number comply with port rules and in that case returns it.
     *
     * @param newPort is the new port number that has to be evaluated to substitute the default one
     * @param defaultPort is the default port number. It has to be a valid port number
     * @return the new port number if valid, the default one otherwise
     */
    protected int portValidator(int newPort, int defaultPort) {
        if(newPort > 1023 && newPort < 65536) return newPort;
        return defaultPort;
    }

    /**
     * Verify that the given ip comply with ipv4 rules and in that case returns it.
     *
     * @param ipToValidate is the new ip that has to be evaluated
     * @param lastValidIp is the last valid ip for the server
     * @return {@code ipToValidate} if it's a valid ip, else return {@code lastValidIp}
     */
    protected String serverIpValidator(String ipToValidate , String lastValidIp) {

        // TODO rivalutare algoritmo

        char[] workingIp = ipToValidate.toCharArray();
        int prevIndex = 0;
        int counter = 0;
        int tmp;
        int workingLength = 15;

        if(workingIp.length < workingLength)
            workingLength = workingIp.length;

        for(int i = 0; i < workingLength; i++) {

            if(workingIp[i] == '.') {
                counter++;

                if(counter > 3)
                    return lastValidIp;

                try {
                    tmp = Integer.parseInt(ipToValidate, prevIndex, i, 10);
                }catch (Exception e){
                    return lastValidIp;
                }

                if(tmp < 0 || tmp > 255)
                    return lastValidIp;

                prevIndex = i+1;
            }
        }

        if(counter < 3)
            return lastValidIp;

        try {
            tmp = Integer.parseInt(ipToValidate, prevIndex, workingIp.length, 10);
        }catch (Exception e){
            return lastValidIp;
        }

        if(tmp < 0 || tmp > 255)
            return lastValidIp;

        return ipToValidate;
    }


    //---------------------------------------------------------------------------------------------
    // Getters

    /**
     * Return the singleton instance of this class.
     * <br>
     * If the instance was never initialized before it will be created assuming there are no startup parameters.
     *
     * @return the instance
     */
    public synchronized static Configuration getInstance() {
        if (instance == null) instance = new Configuration(new String[0]);
        return instance;
    }

    /**
     * Return the singleton instance of this class using the parameters unless the instance already exists.
     *
     * @param args are the parameters used to overwrite the standard server configuration
     * @return the instance
     * @Note: please invoke this method only in {@link Server} class
     */
    public synchronized static Configuration createInstance(String[] args) {
        if (instance == null) instance = new Configuration(args);
        return instance;
    }

    /**
     * Return the RMI port used by the server
     *
     * @return an integer indicating the port
     */
    public int getRmiPort() {
        return rmiPort;
    }

    /**
     * Return the socket port used by the server
     *
     * @return an integer indicating the port
     */
    public int getSocketPort() {
        return socketPort;
    }

    /**
     * Return the interval of time between pings used to evaluate if the connection is still alive.
     *
     * @return an integer indicating the interval in millisecond
     */
    public int getPingTimeInterval() {
        return pingTimeInterval;
    }

    /**
     * Return the maximum amount of missed pings before the connection can be considered dead.
     *
     * @return an integer indicating the amount of pings
     */
    public int getMaxPingCount() {
        return maxPingCount;
    }

    /**
     * Return the duration of a single attempt to read to a socket input
     *
     * @return an int indicating timeout for a read in a socket
     */
    public int getSocketReadTimeout() {
        return socketReadTimeout;
    }

    /**
     * Return the server ip used by the server.
     *
     * @return a {@link String} indicating the port
     */
    public String getServerIp() {
        return serverIp;
    }

    /**
     * Return the executor service used by the server cor thread management.
     *
     * @return an {@link ExecutorService}
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Schedule the {@link ServerPingTask} to be executed repeatedly until cancellation
     *
     * @param serverPingTask is the {@code ServerPingTask} to be scheduled
     */
    public void addTimerTask(ServerPingTask serverPingTask) {
        notLinkedSocketTimer.scheduleAtFixedRate(serverPingTask, pingTimeInterval, pingTimeInterval);
    }

    /**
     * Delete all cancelled {@link ServerPingTask} from the configuration timer
     */
    public void purgeTimer() {
        notLinkedSocketTimer.purge();
    }

    /**
     * Return the time after which the game ends due to loss of players.
     *
     * @return An int indicating the time in milliseconds
     */
    public int getEndGameDueToDisconnectionTimeout() {
        return endGameDueToDisconnectionTimeout;
    }
}
