package messages.network;

import messages.Message;
import messages.MessageType;

/**
 * This message represents the response of the locator to a new connection attempt from a broker or a client.
 * It contains a flag indicating whether the connection is accepted or not. If {@code isConnectionAccepted} returns
 * false, the connection must be closed both by the sender and the receiver.
 */
public class HelloResponseMessage extends Message {
    /** Flag indicating whether the connection is valid or not. */
    private final boolean connectionAccepted;

    public HelloResponseMessage(boolean connectionAccepted) {
        super(MessageType.HELLO_RESPONSE);
        this.connectionAccepted = connectionAccepted;
    }

    public boolean isConnectionAccepted() {
        return connectionAccepted;
    }

    @Override
    public String toString() {
        return "HelloResponseMessage{" +
                "connectionAccepted=" + connectionAccepted +
                '}';
    }
}
