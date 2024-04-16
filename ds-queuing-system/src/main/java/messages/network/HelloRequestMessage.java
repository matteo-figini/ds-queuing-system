package messages.network;

import messages.Message;
import messages.MessageType;

/**
 * This class represents the first message sent from one of the nodes (either a broker or a client).
 */
public class HelloRequestMessage extends Message {
    /** Name of the node. */
    private String nodeName;
    /** Role of the node. */
    private boolean isBroker;

    /**
     * Create the {@code HelloRequestMessage} instance.
     * @param nodeName Name of the connecting node.
     * @param isBroker True if the connecting node is a broker, False otherwise.
     */
    public HelloRequestMessage(String nodeName, boolean isBroker) {
        super(MessageType.HELLO_REQUEST);
        this.nodeName = nodeName;
        this.isBroker = isBroker;
    }

    /**
     * @return The name of the connecting node.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @return True if the connecting node is a broker, False otherwise.
     */
    public boolean isBroker () {
        return isBroker;
    }

    @Override
    public String toString() {
        return "HelloRequestMessage{" +
                "nodeName='" + nodeName + '\'' +
                ", isBroker=" + isBroker +
                '}';
    }
}
