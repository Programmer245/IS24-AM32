package it.polimi.ingsw.am32.message.ClientToServer;

import it.polimi.ingsw.am32.controller.GameController;
import it.polimi.ingsw.am32.controller.GamesManager;
import it.polimi.ingsw.am32.controller.exceptions.FullLobbyException;
import it.polimi.ingsw.am32.controller.exceptions.GameAlreadyStartedException;
import it.polimi.ingsw.am32.controller.exceptions.GameNotFoundException;
import it.polimi.ingsw.am32.message.ServerToClient.AccessGameFailedMessage;
import it.polimi.ingsw.am32.model.exceptions.DuplicateNicknameException;
import it.polimi.ingsw.am32.network.NodeInterface;
import it.polimi.ingsw.am32.network.exceptions.UploadFailureException;

public class AccessGameMessage implements CtoSLobbyMessage {
    private final int matchId;
    private final String senderNickname;

    public AccessGameMessage(int matchId, String senderNickname) {
        this.matchId = matchId;
        this.senderNickname = senderNickname;
    }

    @Override
    public void elaborateMessage(NodeInterface nodeInterface) {
        try {
            GameController gameController = GamesManager.getInstance().accessGame(senderNickname, matchId, nodeInterface);
            nodeInterface.setGameController(gameController);
            // Game was successfully joined
        } catch (GameNotFoundException e) { // Game with given id could not be found; must notify the player trying to join
            try {
                nodeInterface.uploadToClient(new AccessGameFailedMessage(senderNickname, "Game with id " + matchId + " not found"));
            }catch (UploadFailureException e1){
                //TODO: handle exception
            }
        } catch (FullLobbyException e) {
            try{
                nodeInterface.uploadToClient(new AccessGameFailedMessage(senderNickname, "Game with id " + matchId + " is full"));
            }catch (UploadFailureException e2){
                //TODO: handle exception
            }
        } catch (GameAlreadyStartedException e) {
            try{
                nodeInterface.uploadToClient(new AccessGameFailedMessage(senderNickname, "Game with id " + matchId + " has already started"));
            }catch (UploadFailureException e3){
                //TODO: handle exception
            }
        } catch (DuplicateNicknameException e) {
            try {
                nodeInterface.uploadToClient(new AccessGameFailedMessage(senderNickname, "Nickname " + senderNickname + " is already in use"));
            }catch (UploadFailureException e4){
                //TODO: handle exception
            }
        }
    }
}
