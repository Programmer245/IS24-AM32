package it.polimi.ingsw.am32.controller;

import it.polimi.ingsw.am32.controller.exceptions.*;
import it.polimi.ingsw.am32.message.ServerToClient.*;
import it.polimi.ingsw.am32.model.exceptions.DuplicateNicknameException;
import it.polimi.ingsw.am32.model.exceptions.PlayerNotFoundException;
import it.polimi.ingsw.am32.network.ServerNode.ServerNodeInterface;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * This class represents a manager for all the games that are currently being played.
 * Class is a Singleton, meaning that only one instance of it can be created.
 *
 * @author Anto
 */
public class GamesManager {
    /**
     * Logger object of the class
     */
    private static final Logger logger = LogManager.getLogger(GamesManager.class);
    /**
     * instance: The only instance of the class.
     */
    private static GamesManager instance;
    /**
     * games: A list of all the games that are currently being played.
     */
    private final ArrayList<GameController> games;

    private GamesManager() {
        this.games = new ArrayList<>();
    }

    /**
     * Returns the only instance of the class
     *
     * @return The only instance of the class
     */
    public static synchronized GamesManager getInstance() {
        if (instance == null) {
            instance = new GamesManager();
            logger.debug("Instance of GamesManager created");
        }
        return instance;
    }

    /**
     * Creates a new game with the given creator name and player count
     *
     * @param creatorName The name of the player that created the game
     * @param playerCount The number of players that the game will have
     * @param node The server node associated with the given player
     * @return The GameController of the newly created game
     * @throws InvalidPlayerNumberException If the player count is not between 2 and 4
     */
    public synchronized GameController createGame(String creatorName, int playerCount, ServerNodeInterface node) throws InvalidPlayerNumberException {
        logger.debug("Received request to create a new game. Creator name: {}, player count: {}, node: {}", creatorName, playerCount, node);
        if(creatorName == null || creatorName.isBlank()) {
            throw new CriticalFailureException("Creator name cannot be null or empty");
        }
        if(playerCount < 2 || playerCount > 4) {
            throw new InvalidPlayerNumberException("Player count must be between 2 and 4");
        }
        if(node == null) {
            throw new CriticalFailureException("Node cannot be null");
        }

        Random random = new Random();
        int rand = 0;

        boolean foundUnique = false; // Flag indicating whether a valid game id has been found
        while (!foundUnique) { // Loop until a valid game id is found
            rand = random.nextInt(2049); // Generate random id for the game
            foundUnique = true;

            for (GameController game : games) { // Scan all games to check that no other game has the same id
                if (game.getId() == rand) { // Id is not unique
                   foundUnique = false;
                   break;
                }
            }
            // If we reach this point, the id is unique
        }

        GameController game = new GameController(rand, playerCount); // Create a new game instance

        try {
            game.addPlayer(creatorName, node); // Add the creator to the newly created game
            game.submitVirtualViewMessage(new NewGameConfirmationMessage(creatorName, rand));
        } catch (FullLobbyException e) { // It should never happen that the lobby is full when the creator joins. The creator is the first player to join the game.
            throw new CriticalFailureException("Lobby was full when the creator joined the game");
        } catch (VirtualViewNotFoundException e) { // It should never happen that the virtual view of the creator is not found. The creator is the first player to join the game.
            throw new CriticalFailureException("VirtualViewNotFoundException when creator joined the game");
        } catch (DuplicateNicknameException e) { // It should never happen that the creator has a duplicate nickname. The creator is the first player to join the game.
            throw new CriticalFailureException("DuplicateNicknameException when creator joined the game");
        }

        games.add(game); // Add game to the list of all games
        return game;
    }

    /**
     * Adds the player with the given nickname to the game with the given code
     *
     * @param nickname The nickname of the player to be added
     * @param gameCode The code of the game to be accessed
     * @param node The server node associated with the given player
     * @return The GameController of the game with the given code
     * @throws GameNotFoundException If no game with the given code is found
     * @throws FullLobbyException If the lobby of the game is full
     * @throws GameAlreadyStartedException If the game has already started
     * @throws CTRDuplicateNicknameException If the player with the given nickname is already in the game
     */
    public synchronized GameController accessGame(String nickname, int gameCode, ServerNodeInterface node) throws GameNotFoundException, FullLobbyException, GameAlreadyStartedException, CTRDuplicateNicknameException {
        logger.debug("Received request to access game. Nickname: {}, game code: {}, node: {}", nickname, gameCode, node);
        if(nickname == null || nickname.isBlank()) {
            throw new CriticalFailureException("Nickname cannot be null or empty");
        }
        if(node == null) {
            throw new CriticalFailureException("Node cannot be null");
        }

        for (GameController game : games) {
            if (game.getId() == gameCode) { // Found correct GameController instance
                if (game.getStatus() != GameControllerStatus.LOBBY) { // Game is not in the lobby phase as it has already started
                    throw new GameAlreadyStartedException("Game has already started, cannot join now");
                }

                // Game is in the lobby phase
                try {
                    game.addPlayer(nickname, node);
                    game.submitVirtualViewMessage(new AccessGameConfirmMessage(nickname)); // Notify the player that he has joined the game

                    // Notify all players in the lobby of the new player
                    ArrayList<String> allPlayerNicknames = game.getNodeList().stream()
                            .map(PlayerQuadruple::getNickname)
                            .collect(Collectors.toCollection(ArrayList::new));
                    for (PlayerQuadruple playerQuadruple : game.getNodeList()) {
                        // Also notify all players except player that has just connected, that a new player has connected
                        if (!playerQuadruple.getNickname().equals(nickname)) {
                            game.submitVirtualViewMessage(new PlayerConnectedMessage(playerQuadruple.getNickname(), nickname));
                        }
                        game.submitVirtualViewMessage(new LobbyPlayerListMessage(playerQuadruple.getNickname(), allPlayerNicknames));
                    }
                } catch (VirtualViewNotFoundException e) { // Player was added, but his virtual view could not be found
                    throw new CriticalFailureException("VirtualViewNotFoundException when player joined the game");
                } catch (DuplicateNicknameException e) { // Player is not added to the game as he has a duplicate nickname
                    throw new CTRDuplicateNicknameException("Player with nickname " + nickname + " is already in the game");
                }

                if (game.getGameSize() == game.getLobbyPlayerCount()) { // Lobby is now full
                    game.enterPreparationPhase();
                }

                return game;
            }
        }
        throw new GameNotFoundException("No game found with code " + gameCode);
    }

    /**
     * Reconnects the player with the given nickname to the game with the given code
     *
     * @param nickname The nickname of the player to be reconnected
     * @param gameCode The code of the game to be accessed
     * @param node The server node associated with the given player
     * @return The GameController of the game with the given code
     * @throws GameAlreadyEndedException If the game has already ended
     * @throws CTRPlayerNotFoundException If the player with the given nickname is not found in the game
     * @throws GameNotFoundException If no game with the given code is found
     * @throws PlayerAlreadyConnectedException If the player with the given nickname is already connected to the game
     * @throws GameNotYetStartedException If the game has not yet started
     */
    public synchronized GameController reconnectToGame(String nickname, int gameCode, ServerNodeInterface node) throws
            GameAlreadyEndedException, CTRPlayerNotFoundException, GameNotFoundException, PlayerAlreadyConnectedException,
            GameNotYetStartedException
    {
        logger.debug("Received request to reconnect to game. Nickname: {}, game code: {}, node: {}", nickname, gameCode, node);
        if (nickname == null || nickname.isBlank()) {
            throw new CriticalFailureException("Nickname cannot be null or empty");
        }
        if (node == null) {
            throw new CriticalFailureException("Node cannot be null");
        }

        for (GameController game : games) {
            if (game.getId() == gameCode) {
                if (game.getStatus() == GameControllerStatus.GAME_ENDED) { // If the game has already finished, the player cannot reconnect
                    throw new GameAlreadyEndedException("Game has already ended, cannot reconnect now");
                }
                if(game.getStatus() == GameControllerStatus.LOBBY) {
                    throw new GameNotYetStartedException("Game has not yet started, cannot reconnect now. Use accessGame instead.");
                }

                // Game has not yet ended
                try {
                    game.reconnect(nickname, node); // Attempt to reconnect the player

                    for (PlayerQuadruple playerQuadruple : game.getNodeList()) {
                        // Also notify all players except player that has just reconnected, that a player has reconnected
                        if (!playerQuadruple.getNickname().equals(nickname)) {
                            game.submitVirtualViewMessage(new PlayerReconnectedMessage(playerQuadruple.getNickname(), nickname));
                        }
                    }
                } catch (VirtualViewNotFoundException e) {
                    throw new CriticalFailureException("VirtualViewNotFoundException when player reconnected to the game");
                } catch (PlayerNotFoundException e) {
                    throw new CTRPlayerNotFoundException("Player with nickname " + nickname + " not found in the game");
                }

                return game;
            }
        }
        throw new GameNotFoundException("No game found with code " + gameCode);
    }

    /**
     * Return the list of all games that are currently being handled by the server. Used for testing purposes only.
     *
     * @return The list of all games that are currently being handled by the server.
     */
    protected synchronized ArrayList<GameController> getGames() {
        return games;
    }

    /**
     * Clear the instance of the class. Used for testing purposes only.
     */
    protected synchronized void clearInstance() {
        instance = null;
        logger.debug("Instance of GamesManager cleared");
    }
}
