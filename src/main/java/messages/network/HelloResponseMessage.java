package messages.network;

import messages.Message;
import messages.MessageType;

/**
 * This message represents the response of the locator to a new connection attempt from a broker or a client.
 * It contains a flag indicating whether the connection is accepted or not. If {@code isConnectionAccepted} returns
 * false, the connection must be closed both by the sender and the receiver.
 * UPDATE: a new flag {@code nameAlreadyInUse} is used to discriminate the situations in which the connection is accepted
 * by the locator, but the name used by the broker is already in use by another node.
 */
public class HelloResponseMessage extends Message {
    /** Flag indicating whether the connection is valid or not. */
    private final boolean connectionAccepted;
    private final boolean nameAlreadyInUse;

    public HelloResponseMessage(boolean connectionAccepted, boolean nameAlreadyInUse) {
        super(MessageType.HELLO_RESPONSE);
        this.connectionAccepted = connectionAccepted;
        this.nameAlreadyInUse = nameAlreadyInUse;
    }

    public boolean isConnectionNotAccepted() {
        return !connectionAccepted;
    }

    public boolean isNameAlreadyInUse() { return nameAlreadyInUse; }

    @Override
    public String toString() {
        return "HelloResponseMessage{" +
                "connectionAccepted=" + connectionAccepted +
                ", nameAlreadyInUse=" + nameAlreadyInUse +
                '}';
    }
}
