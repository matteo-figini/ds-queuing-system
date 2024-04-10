package messages.network;

import messages.Message;
import messages.MessageType;

import java.io.Serializable;

/**
 * This class represents the first message sent from one of the nodes (either a broker or a client).
 */
public class HelloRequestMessage extends Message {
    /** Name of the node. */
    private String nodeName;
    /** Role of the node. */
    private boolean isBroker;

    public HelloRequestMessage(String nodeName, boolean isBroker) {
        super(MessageType.HELLO_REQUEST);
        this.nodeName = nodeName;
        this.isBroker = isBroker;
    }

    public String getNodeName() {
        return nodeName;
    }

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
