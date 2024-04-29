package it.polimi.ingsw.am32.model;

import it.polimi.ingsw.am32.model.card.NonObjectiveCard;
import it.polimi.ingsw.am32.model.exceptions.*;
import it.polimi.ingsw.am32.model.player.Player;

import java.util.ArrayList;

@SuppressWarnings("ALL")
public interface ModelInterface {
    void enterLobbyPhase();
    void addPlayer(String nickname) throws DuplicateNicknameException;
    void deletePlayer(String nickname) throws PlayerNotFoundException;
    void enterPreparationPhase();
    void assignRandomColoursToPlayers();
    void assignRandomStartingInitialCardsToPlayers();
    int getInitialCardPlayer(String nickname) throws PlayerNotFoundException;
    void createFieldPlayer(String nickname, boolean side) throws PlayerNotFoundException;
    void assignRandomStartingResourceCardsToPlayers();
    void assignRandomStartingGoldCardsToPlayers();
    void pickRandomCommonObjectives();
    void assignRandomStartingSecretObjectivesToPlayers();
    ArrayList<Integer> getSecretObjectiveCardsPlayer(String nickname);
    void receiveSecretObjectiveChoiceFromPlayer(String nickname, int id) throws InvalidSelectionException, PlayerNotFoundException;
    void randomizePlayersOrder();
    void enterPlayingPhase();
    void startTurns();
    void placeCard(int id, int x, int y, boolean side) throws InvalidSelectionException, MissingRequirementsException, InvalidPositionException, PlayerNotFoundException;
    void drawCard(int deckType, int id) throws DrawException, PlayerNotFoundException;
    void nextTurn();
    void setTerminating();
    boolean areWeTerminating();
    boolean isFirstPlayer();
    void setLastTurn();
    void enterTerminatedPhase();
    void addObjectivePoints() throws AlreadyComputedPointsException;
    ArrayList<String> getWinners();
    ArrayList<String> getPlayersNicknames();
    ArrayList<Integer> getCurrentResourcesCards();
    ArrayList<Integer> getCurrentGoldCards();
    ArrayList<Integer> getCommonObjectives();
    int[] getPlayerResources(String nickname) throws PlayerNotFoundException;
    int getPlayerSecretObjective(String nickname) throws PlayerNotFoundException;
    ArrayList<Integer> getPlayerHand(String nickname) throws PlayerNotFoundException;
    ArrayList<int[]> getPlayerField(String nickname) throws PlayerNotFoundException;
    int getCurrentTurnNumber();
    int getMatchStatus();
    int getPlayerColour(String nickname) throws PlayerNotFoundException, NullColourException;
    String getCurrentPlayerNickname();
    ArrayList<Player> getPlayers();
    ArrayList<NonObjectiveCard> getResourceCardsDeck();
    ArrayList<NonObjectiveCard> getGoldCardsDeck();
    int getPlayerPoints(String nickname) throws PlayerNotFoundException;
}