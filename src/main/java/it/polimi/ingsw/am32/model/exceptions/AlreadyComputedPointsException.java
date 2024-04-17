package it.polimi.ingsw.am32.model.exceptions;

/**
 * This class represents a custom exception that is thrown when points have already been computed.
 * It extends the Exception class, meaning it is a checked exception.
 * Checked exceptions need to be declared in a method or constructor's throws clause if they can be thrown by the execution of the method or constructor and propagate outside the method or constructor boundary.
 */
public class AlreadyComputedPointsException extends Exception {
    /**
     * Constructs a new AlreadyComputedPointsException with the specified detail message.
     * The detail message is saved for later retrieval by the Throwable.getMessage() method.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
     */
    public AlreadyComputedPointsException(String message) {
        super(message);
    }
}