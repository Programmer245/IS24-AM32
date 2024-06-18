package it.polimi.ingsw.am32.client;


import it.polimi.ingsw.am32.network.exceptions.ConnectionSetupFailedException;
import it.polimi.ingsw.am32.utilities.IsValid;
import it.polimi.ingsw.am32.client.listener.AskListener;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSLobbyMessage;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSMessage;
import it.polimi.ingsw.am32.network.ClientNode.ClientNodeInterface;
import it.polimi.ingsw.am32.network.ClientNode.RMIClientNode;
import it.polimi.ingsw.am32.network.ClientNode.SKClientNode;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * The View class is an abstract class that serves as a template for the views of the client. It contains common
 * properties and methods that all views should have.
 * <p>
 *    The class provides abstract methods which are implemented by the concrete views. These methods are used to
 *    display the game state to the player, and to request input from the player. The class also provides methods for
 *    updating the game state and the player's information.
 *<p>
 *    The class also provides methods for setting socket client and RMI client, and for notifying the ask listener.
 */
public abstract class View{
    /**
     * The IsValid object that is used to validate the input of the player.
     */
    protected final IsValid isValid;
    /**
     * The ClientNodeInterface object that is used to communicate with the server.
     */
    protected ClientNodeInterface clientNode;
    /**
     * The nickname of the player who is using the client.
     */
    protected String thisPlayerNickname;
    /**
     * The integer that represents the starting card ID of the player.
     */
    protected int startCard;
    /**
     * The integer that represents the game ID of the game that the player is connected to.
     */
    protected int gameID;
    /**
     * The integer that represents the number of players established at the beginning of the game.
     */
    protected int playerNum;
    /**
     * The ArrayList that stores the nicknames of the players in the game.
     */
    protected ArrayList<String> players;
    /**
     * The String that stores the nickname of the current player.
     */
    protected String currentPlayer;
    /**
     * The Event object that stores the current event.
     */
    protected volatile Event currentEvent;
    /**
     * Stores the index of the card placed by the player.
     * When draw confirmation is received, the new card is injected into the hand at the index of the card placed.
     */
    protected int indexCardPlaced=0;
    /**
     * The ArrayList that stores the common objective cards' ID.
     */
    protected ArrayList<Integer> commonObjCards;
    /**
     * The ArrayList that stores the secret objective cards' ID which should be selected by the player.
     */
    protected ArrayList<Integer> secretObjCards;
    /**
     * The integer that stores the secret objective card ID selected by the player.
     */
    protected int secretObjCardSelected;
    /**
     * The ArrayList that stores the IDs of the cards in the player's hand.
     */
    protected ArrayList<Integer> hand;
    /**
     * The ArrayList that stores the IDs of the two resource cards visible at the moment.
     */
    protected ArrayList<Integer> currentResourceCards;
    /**
     * The ArrayList that stores the IDs of the two gold cards visible at the moment.
     */
    protected ArrayList<Integer>  currentGoldCards;
    /**
     * The integer that stores the size of the resource deck.
     */
    protected int resourceDeckSize;
    /**
     * The integer that stores the size of the gold deck.
     */
    protected int goldDeckSize;
    /**
     * The integer that stores the kingdom's type of the resource deck's facing card.
     */
    protected int resourceCardDeckFacingKingdom;
    /**
     * The integer that stores the kingdom's type of the gold deck's facing card.
     */
    protected int goldCardDeckFacingKingdom;
    /**
     * The Event object that stores the status of the game.
     */
    protected volatile Event Status;
    /**
     * The AskListener object that listens for messages from the client to the server.
     */
    protected AskListener askListener;
    /**
     * The ArrayList that stores the available spaces on the field.
     */
    protected ArrayList<int[]> availableSpaces;
    /**
     * The HashMap that stores the public information of the players:save colour,
     * nickname, points and resources of the player.
     */
    protected HashMap<String,PlayerPub> publicInfo;
    /**
     * The ArrayList that stores the objective cards descriptions.
     */
    protected static final ArrayList<ObjectiveCardFactory> objectiveCards = ObjectiveCardFactory.setObjectiveCardArray();
    /**
     * The ArrayList that stores the non-objective cards descriptions.
     */
    protected static final ArrayList<NonObjCardFactory> nonObjCards = NonObjCardFactory.setNonObjCardArray();
    /**
     * The List of ChatMessage objects that stores the chat history of the player.
     */
    protected List<ChatMessage>chatHistory;

    /**
     * The constructor of the class that initializes the view with the default values.
     */
    public View() {
        this.playerNum = 0;
        this.currentPlayer = null;
        this.thisPlayerNickname = null;
        this.gameID = 0;
        this.currentEvent = null;
        this.commonObjCards = null;
        this.secretObjCardSelected = -1;
        this.players = new ArrayList<>();
        this.hand = new ArrayList<>();
        this.publicInfo = new HashMap<>();
        this.chatHistory = Collections.synchronizedList(new ArrayList<>());
        isValid = new IsValid();
    }

    /**
     * This abstract method is used to display the welcome message to the player.
     */
    public abstract void showWelcome();
    /**
     * This abstract method is used to ask the player to choose a connection and insert the server's IP address and
     * port.
     */
    public abstract void chooseConnection();
    /**
     * This method is used to set up a socket client with the specified server IP and port.
     * Also, it creates a new thread to listen for messages from the client.
     * @param serverIP The IP address of the server.
     * @param port The port number of the server.
     * @throws ConnectionSetupFailedException if the connection setup fails.
     */
    public void setSocketClient(String serverIP, int port) throws ConnectionSetupFailedException {
        SKClientNode clientNode = new SKClientNode(this,serverIP,port);
        this.clientNode = clientNode;
        clientNode.startConnection();
        this.askListener = new AskListener(clientNode);
        Thread askListener = new Thread(this.askListener); // Create a new thread listen for messages from the client
        askListener.start();
        //TODO verify if this is correct
    }

    /**
     * This method is used to set up an RMI client with the specified server IP and port.
     * Also, it creates a new thread to listen for messages from the client.
     * @param serverIP The IP address of the server.
     * @param port The port number of the server.
     * @throws ConnectionSetupFailedException if the connection setup fails.
     */
    public void setRMIClient(String serverIP, int port) throws ConnectionSetupFailedException {
        //TODO verify if this is correct
        try{
            RMIClientNode clientNode = new RMIClientNode(this, serverIP, port);
            this.clientNode = clientNode;
            clientNode.startConnection();

            this.askListener = new AskListener(clientNode);
            Thread askListenerThread = new Thread(this.askListener);
            askListenerThread.start();
        }catch (RemoteException e) {
            //TODO
        }
    }
    /**
     * This abstract method is used to ask the player to select the game mode.
     */
    public abstract void askSelectGameMode();
    /**
     * This abstract method is used to ask the player to insert data to create a new game.
     */
    public abstract void askCreateGame();
    /**
     * This abstract method is used to update the game ID and the recipient's nickname after a new game is confirmed.
     * @param gameID The ID of the game.
     * @param recipientNickname The nickname of the recipient.
     */
    public abstract void updateNewGameConfirm(int gameID, String recipientNickname);
    /**
     * This abstract method is used to ask the player to insert data to join a game.
     */
    public abstract void askJoinGame();
    /**
     * This abstract method is used to ask the player to insert data to reconnect to a game.
     */
    public abstract void askReconnectGame();
    /**
     * This abstract method is used to update the list of players in the lobby.
     * @param players The list of players.
     */
    public abstract void updatePlayerList(ArrayList<String> players);

    /**
     * This abstract method is used to set up the player's data once the game is started entering the preparation phase.
     */
    public abstract void setUpPlayersData();

    /**
     * This abstract method is used to update the status of the match when the match status is changed.
     * @param matchStatus The current status of the match.
     */
    public abstract void updateMatchStatus(int matchStatus);

    /**
     * This abstract method is used to request the player to select a side for the starter card.
     * @param ID The ID of the starter card to be selected.
     */
    public abstract void requestSelectStarterCardSide(int ID);


    /**
     * This abstract method is used to place the starter card on the field, update the information of the player's field
     * after placing the starter card, and set the player's colour.
     * @param colour The colour of the player selected randomly by the server.
     * @param cardID The ID of the starter card.
     * @param isUp The side of the starter card selected by the player.
     * @param availablePos The available positions after placing the starter card.
     * @param resources The resources of the player after placing the starter card.
     */
    public abstract void updateConfirmStarterCard(int colour, int cardID, boolean isUp, ArrayList<int[]> availablePos,
                                                  int[] resources);

    /**
     * This abstract method is used to request the player to draw a card.
     */
    public abstract void requestDrawCard();

    /**
     * This abstract method is used to update the player's hand after drawing a card.
     * @param hand The player's hand after drawing a card.
     */
    public abstract void updateAfterDrawCard(ArrayList<Integer> hand);


    /**
     * This abstract method is used to update the deck after drawing a card.
     * @param resourceDeckSize The size of the resource deck.
     * @param goldDeckSize The size of the gold deck.
     * @param currentResourceCards The current resource cards.
     * @param currentGoldCards The current gold cards.
     * @param resourceDeckFace The face of the resource deck.
     * @param goldDeckFace The face of the gold deck.
     */
    public abstract void updateDeck(int resourceDeckSize, int goldDeckSize, int[] currentResourceCards,
                                    int[] currentGoldCards, int resourceDeckFace, int goldDeckFace);

    /**
     * This abstract method is used to convert an integer of colour received from the server to a string of colour.
     * @param colour The colour received from the server.
     * @return the string of colour corresponding to the integer of colour.
     */
    public abstract String convertToColour(int colour);

    /**
     * This abstract method is used to handle a failure case when receiving a failure message from the server.
     * @param event The event type that caused the failure.
     * @param reason The reason for the failure.
     */
    public abstract void handleFailureCase(Event event, String reason);

    /**
     * This abstract method is used to start a chat mode.
     */
    public abstract void startChatting();

    /**
     * This abstract method is used to show the deck.
     */
    public abstract void showDeck();

    /**
     * This abstract method is used to show the useful information of the game to the player.
     */
    public abstract void showHelpInfo();

    /**
     * This abstract method is used to request the player to select a secret objective card.
     */
    public abstract void requestSelectSecretObjectiveCard();

    /**
     * This abstract method is used to save the player's secret objective card after selecting a secret objective card.
     * @param chosenSecretObjectiveCard The ID of the secret objective card selected by the player.
     */
    public abstract void updateConfirmSelectedSecretCard(int chosenSecretObjectiveCard);

    /**
     * This abstract method is used to request the player to place a card.
     */
    public abstract void requestPlaceCard();

    /**
     * This abstract method is used to update the player's field after placing a card.
     * @param playerNickname The nickname of the player who placed the card.
     * @param cardID The ID of the card placed.
     * @param x The x coordinate of the card placed.
     * @param y The y coordinate of the card placed.
     * @param isUp The side of the card placed.
     * @param availablePos The available positions after placing the card.
     * @param resources The resources of the player after placing the card.
     * @param points The points of the player after placing the card.
     */
    public abstract void updateAfterPlacedCard(String playerNickname, int cardID, int x, int y,
                                               boolean isUp, ArrayList<int[]> availablePos, int[] resources,
                                               int points);

    /**
     * This abstract method is used to notify the ask listener of a new CtoS message.
     * @param message The message to be added to the ask listener.
     */
    public void notifyAskListener(CtoSMessage message){
        askListener.addMessage(message);
    }

    /**
     * This abstract method is used to notify the ask listener of a new CtoSLobby message.
     * @param message The message to be added to the ask listener.
     */
    public void notifyAskListener(CtoSLobbyMessage message){
        askListener.addMessage(message);
    }

    /**
     * This method is used to update the current event with the new event.
     * @param event The new event to be updated.
     */
    public void updateCurrentEvent(Event event){
        this.currentEvent = event;
    }
    /**
     * This abstract method is used to launch the view.
     */
    public abstract void launch();
    /**
     * This method is used to get the current event.
     * @return The current event.
     */
    public Event getEvent(){
        return this.currentEvent;
    }
    /**
     * This abstract method is used to show the field of a player.
     * @param playerNickname The nickname of the player whose field is to be shown.
     */
    public abstract void showPlayersField(String playerNickname);

    /**
     * This abstract method is used to show the points and resources of a player.
     * @param playerNickname The nickname of the player whose points and resources are to be shown.
     */
    public abstract void showPointsAndResource(String playerNickname);

    /**
     * This abstract method is used to set the cards received by the server.
     * @param secrets The secret cards received by the server.
     * @param common The common cards received by the server.
     * @param hand The cards in the player's hand received by the server.
     */
    public abstract void setCardsReceived(ArrayList<Integer> secrets, ArrayList<Integer> common,
                                          ArrayList<Integer> hand);
    /**
     * This abstract method is used to show the cards in player's hand.
     */
    public abstract void showHand();
    /**
     * This abstract method is used to show a specific card with the given ID and side.
     * @param ID The ID of the card.
     * @param isUp The side of the card.
     */
    public abstract void showCard(int ID, boolean isUp);

    /**
     * This abstract method is used to update the current player after the player's turn is changed.
     * @param playerNickname The nickname of the current player.
     */
    public abstract void updatePlayerTurn(String playerNickname);
    /**
     * This abstract method is used to update the player's data when the game enters the preparation phase or the player
     * reconnects to the game.
     * @param playerNicknames The nicknames of the players.
     * @param playerConnected The connection status of the players.
     * @param playerColours The colours of the players.
     * @param playerHand The cards in the player's hand.
     * @param playerSecretObjective The secret objective of the player.
     * @param playerPoints The points of the player.
     * @param playerFields The fields' information of the players.
     * @param playerResources The resources of this player.
     * @param gameCommonObjectives The common objectives of the game.
     * @param gameCurrentResourceCards The current resource cards of the game.
     * @param gameCurrentGoldCards The current gold cards of the game.
     * @param gameResourcesDeckSize The resource deck size of the game.
     * @param gameGoldDeckSize The gold deck size of the game.
     * @param matchStatus The status of the match.
     * @param chatHistory The chat history of the game.
     * @param currentPlayer The current player of the game.
     * @param newAvailableFieldSpaces The new available spaces on the field.
     * @param resourceCardDeckFacingKingdom The kingdom facing the resource card deck.
     * @param goldCardDeckFacingKingdom The kingdom facing the gold card deck.
     * @param playersResourcesSummary The resources of the players.
     * @param playerAssignedSecretObjectiveCards The secret objective cards assigned to the player.
     * @param playerStartingCard The starting card of the player.
     */
    public abstract void updatePlayerData(ArrayList<String> playerNicknames, ArrayList<Boolean> playerConnected,
                                          ArrayList<Integer> playerColours, ArrayList<Integer> playerHand,
                                          int playerSecretObjective, int[] playerPoints,
                                          ArrayList<ArrayList<int[]>> playerFields, int[] playerResources,
                                          ArrayList<Integer> gameCommonObjectives,
                                          ArrayList<Integer> gameCurrentResourceCards,
                                          ArrayList<Integer> gameCurrentGoldCards, int gameResourcesDeckSize,
                                          int gameGoldDeckSize, int matchStatus, ArrayList<String[]> chatHistory,
                                          String currentPlayer, ArrayList<int[]> newAvailableFieldSpaces,
                                          int resourceCardDeckFacingKingdom, int goldCardDeckFacingKingdom,
                                          ArrayList<int[]> playersResourcesSummary,
                                          ArrayList<Integer> playerAssignedSecretObjectiveCards,
                                          int playerStartingCard) ;

    /**
     * This abstract method is used to update the field of the player when receiving a confirmation message from
     * the server.
     * @param playerNickname The nickname of the player who placed the card.
     * @param placedCard The card placed by the player.
     * @param placedCardCoordinates The coordinates of the placed card.
     * @param placedSide The side of the placed card.
     * @param playerPoints The points of the player after placing the card.
     * @param playerResources The resources of the player after placing the card.
     * @param newAvailableFieldSpaces The new available spaces on the field after placing the card.
     */
    public abstract void updatePlacedCardConfirm(String playerNickname, int placedCard, int[] placedCardCoordinates,
                                                 boolean placedSide, int playerPoints, int[] playerResources,
                                                 ArrayList<int[]> newAvailableFieldSpaces);

    /**
     * This abstract method is used to show the winners and the final points of the players when the match ends.
     * @param players The players of the match.
     * @param points The final points of the players.
     * @param secrets The secret cards of the players.
     * @param pointsGainedFromSecrets The points gained from the objective cards.
     * @param winners The winners of the match.
     */
    public abstract void showMatchWinners(ArrayList<String> players, ArrayList<Integer> points,
                                          ArrayList<Integer> secrets, ArrayList<Integer> pointsGainedFromSecrets,
                                          ArrayList<String> winners);

    /**
     * This abstract method is used to remove the card from the player's field when the player is disconnected before
     * the draw card.
     * @param playerNickname The nickname of the player who is disconnected.
     * @param removedCard The card removed from the player's field.
     * @param playerPoints The points of the player after removing the card.
     * @param playerResources The resources of the player after removing the card.
     */
    public abstract void updateRollback(String playerNickname, int removedCard, int playerPoints,
                                        int[] playerResources);
    /**
     * This abstract method is used to show the chat history of the player.
     * @param chatHistory The chat history of the player.
     */
    public abstract void showChatHistory(List<ChatMessage> chatHistory);
    /**
     * This abstract method is used to update the chat history when receiving a new chat message.
     * @param recipientString The recipient of the chat message.
     * @param senderNickname The sender of the chat message.
     * @param content The content of the chat message.
     */
    public abstract void updateChat(String recipientString, String senderNickname, String content);
    /**
     * This abstract method is used to set the starter card ID.
     * @param cardId The ID of the starter card.
     */
    public abstract void setStarterCard(int cardId);
    /**
     * This abstract method is used to handle an event.
     * @param event The event to handle.
     * @param nickname The nickname of the player will be used to handle the event.
     */
    public abstract void handleEvent(Event event, String nickname);

    public abstract void nodeDisconnected();

    public abstract void nodeReconnected();
}


