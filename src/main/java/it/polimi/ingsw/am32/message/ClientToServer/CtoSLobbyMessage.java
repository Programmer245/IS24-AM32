package it.polimi.ingsw.am32.message.ClientToServer;

import it.polimi.ingsw.am32.controller.GameController;
import it.polimi.ingsw.am32.controller.GamesManager;
import it.polimi.ingsw.am32.controller.exceptions.*;
import it.polimi.ingsw.am32.model.exceptions.DuplicateNicknameException;
import it.polimi.ingsw.am32.model.exceptions.PlayerNotFoundException;
import it.polimi.ingsw.am32.network.ServerNode.NodeInterface;

import java.io.InvalidClassException;
import java.io.Serializable;

/**
 * This interface represents a message from the client to the server in the lobby phase.
 * It contains a single method to elaborate the message
 */
public interface CtoSLobbyMessage extends Serializable {
    GameController elaborateMessage(NodeInterface nodeInterface) throws InvalidPlayerNumberException, GameAlreadyStartedException, FullLobbyException, DuplicateNicknameException, GameNotFoundException, GameAlreadyEndedException, PlayerNotFoundException;
}
