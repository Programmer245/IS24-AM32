package it.polimi.ingsw.am32.network;

import it.polimi.ingsw.am32.controller.GameController;
import it.polimi.ingsw.am32.message.Message;
import it.polimi.ingsw.am32.message.StoCMessage;

public class RMIServerNode implements RMIServerNodeInt, NodeInterface {
    private GameController gameController;
    private int pingCount;
    public RMIServerNode(GameController gameController) {
        this.gameController = gameController;
    }
    public void uploadCtoS(Message message) {
        //TODO
    }
    public void uploadToClient(StoCMessage message) {
        //TODO
    }

    public void pingTimeOverdue() {
        //TODO
    }
    public void resetTimeCounter(){
        //TODO
    }

}
