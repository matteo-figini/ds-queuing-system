package messages;

import java.io.Serializable;

/**
 * This is the base class for messages to be sent in the raft algorithm.
 */
public class Message implements Serializable {
    public final MessageType type;

    public Message (MessageType type) {
        this.type = type;
    }
}
