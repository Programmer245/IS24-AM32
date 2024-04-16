package it.polimi.ingsw.am32.model.player;

import it.polimi.ingsw.am32.model.card.Card;
import it.polimi.ingsw.am32.model.card.NonObjectiveCard;
import it.polimi.ingsw.am32.model.card.pointstrategy.PointStrategy;
import it.polimi.ingsw.am32.model.field.Field;

import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * This class is responsible for managing the player's actions and status.
 * @author Matteo
 */
public class Player {

    //---------------------------------------------------------------------------------------------
    // Variables and Constants

    private final String nickname;
    private Field gameField;
    private Card secretObjective;
    private Colour colour;
    private int points;
    private ArrayList<NonObjectiveCard> hand;
    private final Card[] tmpSecretObj;
    private int pointsGainedFromObjectives = 0;
    private final boolean[] objectivePointsState = new boolean[]{false, false};

    private final static int secObjOptions = 2;


    //---------------------------------------------------------------------------------------------
    // Constructors

    /**
     * Initialize the player
     *
     * @param nickname name of the player
     */
    public Player(String nickname) {
        this.nickname = nickname;
        this.gameField = null;
        this.points = 0;
        this.secretObjective = null;
        this.hand = null;
        this.colour = null;
        this.tmpSecretObj = new Card[secObjOptions];
    }


    //---------------------------------------------------------------------------------------------
    // Methods

    /**
     * Place the initial card in the hand of the player
     *
     * @param initialCard is the initial card assigned by the match
     * @return true if the assignment was successful, false if the hand wasn't empty and the assignment failed
     */
    public boolean assignStartingCard(NonObjectiveCard initialCard) {

        if(hand != null)
            return false;

        hand = new ArrayList<>();
        hand.addLast(initialCard);
        return true;
    }


    /**
     * Create the field and place the initial card in it
     *
     * @param isUp denote the side of the card chosen by the player
     * @return true if the process was successful, false if the method was already executed
     */
    public boolean initializeGameField(boolean isUp) {

        if (gameField != null || hand == null)
            return false;

        NonObjectiveCard tmpCard;

        try {
            tmpCard = hand.getFirst();
        } catch (NoSuchElementException e) {
            return false;
        }

        gameField = new Field(tmpCard, isUp);
        hand.clear();
        return true;
    }


    /**
     * Search in the player hand for the card that has to be selected and saved as the secret objective
     *
     * @param id of the card chosen by the player
     * @return true if the card was successfully chosen, false if the card wasn't in the hand of the player
     */
    public boolean secretObjectiveSelection(int id) {
        if(tmpSecretObj[0].getId() == id) {
            secretObjective = tmpSecretObj[0];
        } else if (tmpSecretObj[1].getId() == id){
            secretObjective = tmpSecretObj[1];
        } else
            return false;

        return true;
    }


    /**
     * Place the two card given in the parameters in the hand so that that one of them can be chosen as secret objective
     *
     * @param firstCard is the first choice for the secret objective
     * @param secondCard is the second choice for the secret objective
     * @return true if the cards where successfully saved for later selection, false if not
     */
    public boolean receiveSecretObjective(Card firstCard, Card secondCard) {

        if(tmpSecretObj[0] != null || tmpSecretObj[1] != null)
            return false;

        tmpSecretObj[0] = firstCard;
        tmpSecretObj[1] = secondCard;
        return true;
    }


    /**
     * tries to put a card in the hand of the player
     *
     * @param newCard is the card that has to be added to the hand of the player
     * @return true if successfully added, false if not
     */
    public boolean putCardInHand(NonObjectiveCard newCard) {

        if(hand == null || hand.size() >= 3)
            return false;

        hand.addLast(newCard);
        return true;
    }

    /**
     * Tries to take a card from the hand of the player and place it in the field. If the card is placed successfully
     * calculate the points gained from its placement and add them to those of the player
     *
     * @param id is the id of the card in the hand of the player that has to be placed
     * @param x is the horizontal coordinate of the position
     * @param y is the vertical coordinate of the position
     * @param isUp is the side of the card that is going to be visible when placed
     * @return true if the process was successful, false if not
     */
    public boolean performMove(int id, int x, int y, boolean isUp) {

        if(gameField == null)
            return false;

        NonObjectiveCard nonObjectiveCard = null;
        int tmpVar = -1;

        for (int i = 0; i < hand.size(); i++)
            if(hand.get(i).getId() == id) {
                nonObjectiveCard = hand.get(i);
                tmpVar = i;
                break;
            }
        
        if(nonObjectiveCard == null)
            return false;
        
        boolean result = gameField.placeCardInField(nonObjectiveCard, x, y, isUp);

        if(!result)
            return false;

        hand.remove(tmpVar);

        // All the placeable cards: Gold, Resource and Starting cannot give
        // points to the player if they are placed with their back-up.

        if(isUp){
            PointStrategy pointStrategy = nonObjectiveCard.getPointStrategy();

            int occurrences = pointStrategy.calculateOccurrences(gameField, x, y);

            points += occurrences * nonObjectiveCard.getValue();
        }

        return true;
    }

    /**
     * Use the given cards to calculate the extra points that the player gains from the common objectives and add them
     * to the current personal points
     *
     * @param objectiveCards are the common objectives used to calculate the extra points
     * @return true if the points were successfully calculated and added, false if not
     */
    public boolean updatePointsForObjectives(Card[] objectiveCards) {

        if(objectiveCards[0].getPointStrategy() == null || objectiveCards[1].getPointStrategy() == null)
            return false;

        if(objectivePointsState[0])
            return false;

        PointStrategy pointStrategy1 = objectiveCards[0].getPointStrategy();
        PointStrategy pointStrategy2 = objectiveCards[1].getPointStrategy();

        int multiplierPoints1 = pointStrategy1.calculateOccurrences(gameField, 0,0);
        int multiplierPoints2 = pointStrategy2.calculateOccurrences(gameField, 0,0);

        int value1 = objectiveCards[0].getValue();
        int value2 = objectiveCards[1].getValue();

        int tmpGain = value1 * multiplierPoints1 + value2 * multiplierPoints2;

        pointsGainedFromObjectives += tmpGain;

        objectivePointsState[0] = true;

        points += tmpGain;

        return true;
    }

    /**
     * Calculate the points gained by the player for the secret objective and add them to his current points
     *
     * @return true if the process is successful, false if not
     */
    public boolean updatePointsForSecretObjective() {

        PointStrategy pointStrategy = secretObjective.getPointStrategy();

        if(pointStrategy == null)
            return false;

        if(objectivePointsState[1])
            return false;

        int multiplierPoints = pointStrategy.calculateOccurrences(gameField, 0,0);

        int value = secretObjective.getValue();

        int tmpGain = value * multiplierPoints;

        pointsGainedFromObjectives += tmpGain;

        objectivePointsState[1] = true;

        points += tmpGain;

        return true;
    }

    //---------------------------------------------------------------------------------------------
    // Getters

    /**
     * Getter:
     * @return the nickname of the player
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Getter:
     *
     * @return the field if initialized, null if not
     */
    public Field getField() {
        return gameField;
    }

    /**
     * Getter:
     *
     * @return a Card if the secretObjective has already been selected, null if not
     */
    public Card getSecretObjective() {
        return secretObjective;
    }

    /**
     * Getter:
     *
     * @return the color of the player
     */
    public Colour getColour() {
        return colour;
    }

    /**
     * Getter:
     *
     * @return the current points of the player
     */
    public int getPoints() {
        return points;
    }

    /**
     * Getter:
     *
     * @return the hand of the player
     */
    public ArrayList<NonObjectiveCard> getHand() {
        return hand;
    }

    /**
     * Find the initial card, if assigned, and returns it
     *
     * @return The initial Card, if it was non already assigned returns null
     */
    public NonObjectiveCard getInitialCard() {
        if( gameField != null)
            return gameField.getCardFromPosition(0,0);
        else {
            try {
                return hand.getFirst();
            } catch (NoSuchElementException e){
                return null;
            }
        }
    }

    /**
     * Getter:
     *
     * @return the objective cards received by player.
     */
    public Card[] getTmpSecretObj() {
        return tmpSecretObj;
    }

    public int getPointsGainedFromObjectives() {
        return pointsGainedFromObjectives;
    }

    //---------------------------------------------------------------------------------------------
    // Setters

    /**
     * Setter
     *
     * @param points is the value to be set as number of points of the player
     */
    public void setPoints(int points) {
        this.points = points;
    }

    /**
     * assign a colour to the player if it doesn't have already one
     *
     * @param colour is the colour of the player
     * @return true if successfully assigned, false if not
     */
    public boolean setColour(Colour colour) {
        if(this.colour != null)
            return false;

        this.colour = colour;
        return true;
    }

}