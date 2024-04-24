package it.polimi.ingsw.am32.message.ClientToServer;

import it.polimi.ingsw.am32.controller.GameController;

public class RequestGameStatusMessage implements CtoSMessage {
    private final String senderNickname;

    public RequestGameStatusMessage(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    @Override
    public void elaborateMessage(GameController gameController) {
        // TODO
    }
}
