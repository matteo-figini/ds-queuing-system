package messages;

/**
 * This is the base class for messages to be sent in the raft algorithm.
 */
public class Message {
    public final MessageType type;

    public Message(MessageType type) {
        this.type = type;
    }
}
