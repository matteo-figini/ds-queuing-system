package application.exceptions;

/**
 * Thrown when a user reaches the end of the queue where
 * is trying to read.
 */
public class EndOfQueueException extends Exception {
    public EndOfQueueException(String message){
        super(message);
    }
}
