package messages.network;

import messages.Message;
import messages.MessageType;

/**
 * This class represents the first message sent from one of the nodes (either a broker or a client).
 */
public class HelloRequestMessage extends Message {
    private final String nodeIPAddress;
    private final int nodePublicPort;
    private final String nodeName;
    private final boolean isBroker;

    /**
     * Create the {@code HelloRequestMessage}.
     * @param nodeIPAddress IP Address of the sender node.
     * @param nodePublicPort Public port of the sender node on which the {@code ServerSocket} is running.
     * @param nodeName Name of the sender node.
     * @param isBroker Flag indicating if the sender node is a broker (True) or a client (False).
     */
    public HelloRequestMessage(String nodeIPAddress, int nodePublicPort, String nodeName, boolean isBroker) {
        super(MessageType.HELLO_REQUEST);
        this.nodeIPAddress = nodeIPAddress;
        this.nodePublicPort = nodePublicPort;
        this.nodeName = nodeName;
        this.isBroker = isBroker;
    }

    /**
     * @return IP Address of the sender node.
     */
    public String getNodeIPAddress() {
        return nodeIPAddress;
    }

    /**
     * @return Public port of the sender node on which the {@code ServerSocket} is running.
     */
    public int getNodePublicPort() {
        return nodePublicPort;
    }

    /**
     * @return Name of the sender node.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @return Flag indicating if the sender node is a broker (True) or a client (False).
     */
    public boolean isBroker() {
        return isBroker;
    }

    @Override
    public String toString() {
        return "HelloRequestMessage{" +
                "nodeIPAddress='" + nodeIPAddress + '\'' +
                ", nodePublicPort=" + nodePublicPort +
                ", nodeName='" + nodeName + '\'' +
                ", isBroker=" + isBroker +
                '}';
    }
}
