package it.polimi.ingsw.am32.message.ClientToServer;

import it.polimi.ingsw.am32.controller.GameController;

/**
 * This class is used to manage the message sent by the client when he wants to know the status of the game.
 */
public class RequestGameStatusMessage implements CtoSMessage {
    /**
     * The nickname of the player who wants to know the status of the game
     */
    private final String senderNickname;

    /**
     * Constructor: a message representing a request for the status of the game sent by a player.
     * @param senderNickname the nickname of the player who wants to know the status of the game
     *
     */
    public RequestGameStatusMessage(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    /**
     * This method is called when a player wants to know the status of the game.
     * Sends the status of the game to the player.
     * @param gameController the game controller of the game
     */
    @Override
    public void elaborateMessage(GameController gameController) {
        gameController.sendGameStatus(senderNickname);
    }

    /**
     * This method overrides the default toString method.
     * It provides a string representation of a message object, which can be useful for debugging purposes.
     *
     * @return A string representation of the RequestGameStatusMessage object.
     * The string includes the message type and the senderNickname properties of the object.
     */
    @Override
    public String toString() {
        return "RequestGameStatusMessage:{" +
                "senderNickname='" + senderNickname + '\'' +
                '}';
    }
}
