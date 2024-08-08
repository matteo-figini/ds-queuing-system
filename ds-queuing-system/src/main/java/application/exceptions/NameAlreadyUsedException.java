package application.exceptions;

import javax.naming.Name;

/**
 * Thrown when a user tries to create a queue using a name
 * that is already used by another queue.
 */
public class NameAlreadyUsedException extends Exception{
    public NameAlreadyUsedException(String message){
        super(message);
    }
}
