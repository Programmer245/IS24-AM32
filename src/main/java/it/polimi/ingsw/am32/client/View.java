package it.polimi.ingsw.am32.client;


import it.polimi.ingsw.am32.utilities.IsValid;
import it.polimi.ingsw.am32.client.ChatMessage;
import it.polimi.ingsw.am32.client.listener.AskListener;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSLobbyMessage;
import it.polimi.ingsw.am32.message.ClientToServer.CtoSMessage;
import it.polimi.ingsw.am32.network.ClientNode.ClientNodeInterface;
import it.polimi.ingsw.am32.network.ClientNode.RMIClientNode;
import it.polimi.ingsw.am32.network.ClientNode.SKClientNode;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public abstract class View{
    protected final IsValid isValid = new IsValid();
    protected ClientNodeInterface clientNode;
    protected String thisPlayerNickname;
    protected int startCard;
    protected int gameID; //save the game ID received from the NewGameConfirmationMessage or AccessGameConfirmMessage.
    protected int playerNum; //number of players connected to the game, if the player is disconnected, the number will
                             // decrease.
    protected ArrayList<String> players; //save and update the players in the game.
    protected String currentPlayer; //save and update the current player by receiving the message from the server.
    protected volatile Event currentEvent; //TODO: not sure if this is useful
    /**
     * Stores the index of the card placed by the player.
     * When draw confirmation is received, the new card is injected into the hand at the index of the card placed.
     */
    protected int indexCardPlaced=0;
    protected ArrayList<Integer> commonObjCards;
    protected ArrayList<Integer> secretObjCards;
    protected int secretObjCardSelected;
    protected ArrayList<Integer> hand;
    protected ArrayList<Integer> currentResourceCards;
    protected ArrayList<Integer>  currentGoldCards;
    protected int resourceDeckSize;
    protected int goldDeckSize;
    protected int resourceCardDeckFacingKingdom;
    protected int goldCardDeckFacingKingdom;
    protected volatile Event Status;
    protected AskListener askListener;
    protected ArrayList<int[]> availableSpaces;
    protected HashMap<String,PlayerPub> publicInfo; //save the colour, nickname, points and resources of the player.
    protected static final ArrayList<ObjectiveCardFactory> objectiveCards = ObjectiveCardFactory.setObjectiveCardArray();
    protected static final ArrayList<NonObjCardFactory> nonObjCards = NonObjCardFactory.setNonObjCardArray();
    protected List<ChatMessage>chatHistory;
    protected boolean chatMode = false;
    protected volatile boolean isMyTurn = true;
    protected volatile boolean isInThread = false;
    public View() {
        this.playerNum = 0;
        this.currentPlayer = null;
        this.thisPlayerNickname = null;
        this.gameID = 0;
        this.currentEvent = null;
        this.commonObjCards = null;
        this.players = new ArrayList<>();
        this.hand = new ArrayList<>();
        this.publicInfo = new HashMap<>();
        this.chatHistory = Collections.synchronizedList(new ArrayList<>());
    }

    public abstract void showWelcome();

    public abstract void askSelectGameMode();

    public abstract void askCreateGame();

    public abstract void updateNewGameConfirm(int gameID, String recipientNickname);

    public abstract void askJoinGame();

    public abstract void askReconnectGame();


    public abstract void chooseConnection();

    public void setSocketClient(String serverIP, int port) throws IOException {
        SKClientNode clientNode = new SKClientNode(this,serverIP,port);
        this.clientNode = clientNode;
        clientNode.startConnection();
        this.askListener = new  AskListener(clientNode);
        Thread askListener = new Thread(this.askListener);  // Create a new thread to listen for messages from the client
        askListener.start();
        //TODO verify if this is correct
    }
    public void setRMIClient(String serverIP, int port) {
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

    public abstract void updatePlayerList(ArrayList<String> players);

    public abstract void setUpPlayersData();

    public abstract void updateMatchStatus(int matchStatus);

    public abstract void requestSelectStarterCardSide(int ID);


    public abstract void updateConfirmStarterCard(int colour, int cardID, boolean isUp, ArrayList<int[]> availablePos, int[] resources);

    public abstract void requestDrawCard();

    //-------------------Game start-----------------------


    public abstract void updateAfterDrawCard(ArrayList<Integer> hand);

    public abstract void updateDeck(int resourceDeckSize, int goldDeckSize, int[] currentResourceCards,
                                    int[] currentGoldCards, int resourceDeckFace, int goldDeckFace);

    public abstract String convertToColour(int colour);

    public abstract void handleFailureCase(Event event, String reason);

    public abstract void startChatting();

    public abstract void showDeck();

    public abstract void showHelpInfo();

    //-------------------Game start-----------------------


    public abstract void requestSelectSecretObjectiveCard();

    public abstract void updateConfirmSelectedSecretCard(int chosenSecretObjectiveCard);
    

    public abstract void requestPlaceCard();


    public abstract void updateAfterPlacedCard(String playerNickname, int cardID, int x, int y,
                                               boolean isUp, ArrayList<int[]> availablePos, int[] resources,
                                               int points);
    public void notifyAskListener(CtoSMessage message){
        askListener.addMessage(message);
    }
    public void notifyAskListener(CtoSLobbyMessage message){
        askListener.addMessage(message);
    }
    public void updateCurrentEvent(Event event){
        this.currentEvent = event;
    }

    public abstract void launch();
    public Event getEvent(){
        return this.currentEvent;
    }

    public abstract void showPlayersField(String playerNickname);

    public abstract void showPointsAndResource(String playerNickname);

    public abstract void setCardsReceived(ArrayList<Integer> secrets, ArrayList<Integer> common, ArrayList<Integer> hand);

    public abstract void showHand();

    public abstract void showCard(int ID, boolean isUp);

    public abstract void updatePlayerTurn(String playerNickname);

    public abstract void updatePlayerData(ArrayList<String> playerNicknames, ArrayList<Boolean> playerConnected,
                                 ArrayList<Integer> playerColours, ArrayList<Integer> playerHand,
                                 int playerSecretObjective, int[] playerPoints,
                                 ArrayList<ArrayList<int[]>> playerFields, int[] playerResources,
                                 ArrayList<Integer> gameCommonObjectives, ArrayList<Integer> gameCurrentResourceCards,
                                 ArrayList<Integer> gameCurrentGoldCards, int gameResourcesDeckSize,
                                 int gameGoldDeckSize, int matchStatus, ArrayList<String[]> chatHistory,
                                 String currentPlayer, ArrayList<int[]> newAvailableFieldSpaces, int resourceCardDeckFacingKingdom, int goldCardDeckFacingKingdom, ArrayList<int[]> playersResourcesSummary) ;

    public abstract void updatePlacedCardConfirm(String playerNickname, int placedCard, int[] placedCardCoordinates, boolean placedSide, int playerPoints, int[] playerResources, ArrayList<int[]> newAvailableFieldSpaces);

    public abstract void showMatchWinners(ArrayList<String> players, ArrayList<Integer> points, ArrayList<Integer> secrets, ArrayList<Integer> pointsGainedFromSecrets, ArrayList<String> winners);

    public abstract void updateRollback(String playerNickname, int removedCard, int playerPoints, int[] playerResources);

    public abstract void showChatHistory(List<ChatMessage> chatHistory);

    public abstract void updateChat(String recipientString, String senderNickname, String content);

    public abstract void setStarterCard(int cardId);

    public abstract void handleEvent(Event event, String nickname);
}


