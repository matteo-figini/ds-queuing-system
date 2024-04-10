package locator;

import messages.Message;

import java.net.InetAddress;
import java.net.Socket;

/**
 * This class is a representation of any tipe of node, both brokers and clients connected to the {@code Locator}.
 * It contains information useful to differentiate between the broker and the other clients.
 */
public class NodeReference {
    /** Name of the node. This is assumed to be unique in the entire network. */
    private final String nodeName;
    /** Socket representing the connection of the locator with the node. */
    private Socket nodeSocket;
    /** Type of the node (whether it is a broker or not - hence a client. */
    private boolean isBroker = false;
    /** Flag indicating if it is a leader or not (consistent only if isBroker == true). */
    private boolean isLeaderOfBrokers = false;
    /** Flag indicating if the node is currently connected. */
    private boolean isConnected = true;

    /**
     * Instantiate the {@code NodeReference} element.
     * @param nodeName Name of the node.
     */
    public NodeReference (String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * Instantiate the {@code NodeReference} element.
     * @param nodeName Name of the node.
     * @param nodeSocket {@code Socket} representing the connection of the node.
     */
    public NodeReference(String nodeName, Socket nodeSocket) {
        this(nodeName);
        this.nodeSocket = nodeSocket;
    }

    /**
     * Instantiate the {@code NodeReference} element.
     * @param nodeName Name of the node.
     * @param nodeSocket {@code Socket} representing the connection of the node.
     * @param isBroker Flag indicating if the node is a broker or not (thus, a client).
     */
    public NodeReference(String nodeName, Socket nodeSocket, boolean isBroker) {
        this(nodeName, nodeSocket);
        this.isBroker = isBroker;
    }

    /* ------------ APPLICATION METHODS ---------- */


    /* ------------ SETTERS ---------- */
    public void setBroker(boolean broker) {
        isBroker = broker;
    }

    public void setLeaderOfBrokers(boolean leaderOfBrokers) {
        isLeaderOfBrokers = leaderOfBrokers;
    }

    /* ---------- GETTERS ---------- */
    public String getNodeName() {
        return nodeName;
    }

    public Socket getNodeSocket() { return nodeSocket; }

    public InetAddress getNodeSocketAddress() { return nodeSocket.getInetAddress(); }

    public int getNodeSocketPort() { return nodeSocket.getPort(); }

    public boolean isBroker() { return isBroker; }

    public boolean isLeaderOfBrokers() { return isLeaderOfBrokers; }

    /* ---------- UTILITIES ---------- */

    @Override
    public String toString() {
        return "NodeReference{" +
                "nodeName='" + nodeName + '\'' +
                ", nodeSocket=" + nodeSocket +
                ", isBroker=" + isBroker +
                ", isLeaderOfBrokers=" + isLeaderOfBrokers +
                '}';
    }
}
