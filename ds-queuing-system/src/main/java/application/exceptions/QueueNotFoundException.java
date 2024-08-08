package application.exceptions;

/**
 * Thrown when a user requires an operation on a queue name
 * that doesn't correspond with any of the active queues.
 */
public class QueueNotFoundException extends Exception{
    public QueueNotFoundException(String message){
        super(message);
    }
}
