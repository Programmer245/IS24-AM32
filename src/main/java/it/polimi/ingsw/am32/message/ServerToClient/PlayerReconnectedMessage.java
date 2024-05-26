package it.polimi.ingsw.am32.message.ServerToClient;

import it.polimi.ingsw.am32.client.View;

public class PlayerReconnectedMessage implements StoCMessage {
    private final String recipientNickname;
    private final String disconnectedNickname;

    public PlayerReconnectedMessage(String recipientNickname, String disconnectedNickname) {
        this.recipientNickname = recipientNickname;
        this.disconnectedNickname = disconnectedNickname;
    }

    @Override
    public void processMessage(View view) {
        // TODO
    }

    @Override
    public String getRecipientNickname() {
        return recipientNickname;
    }

    @Override
    public String toString() {
        return "PlayerReconnectedMessage:{" +
                "recipientNickname='" + recipientNickname + '\'' +
                ", disconnectedNickname='" + disconnectedNickname + '\'' +
                '}';
    }
}