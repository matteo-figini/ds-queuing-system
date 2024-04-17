package locator;

import java.io.Serializable;

/**
 * This class represents a description of a node reference from the point of view of the locator.
 * Every connected node has its reference to the {@code NodeHandler} class, a {@code String} representing
 * the node name and a boolean flag indicating whether the node is a broker or not (True if the node is broker,
 * False if the node is a client.
 */
public class NodeReference implements Serializable {
    private String ipAddress;
    private int publicPort;
    private String nodeName;
    private boolean isBroker;

    public NodeReference(String ipAddress, int publicPort, String nodeName, boolean isBroker) {
        this.publicPort = publicPort;
        this.ipAddress = ipAddress;
        this.nodeName = nodeName;
        this.isBroker = isBroker;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean isBroker() {
        return isBroker;
    }

    @Override
    public String toString() {
        return "NodeReference{" +
                "ipAddress='" + ipAddress + '\'' +
                ", publicPort=" + publicPort +
                ", nodeName='" + nodeName + '\'' +
                ", isBroker=" + isBroker +
                '}';
    }
}
