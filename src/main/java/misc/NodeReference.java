package misc;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents a description of a node reference from the point of view of the locator.
 * Every connected node has its reference to the {@code NodeHandler} class, a {@code String} representing
 * the node name and a boolean flag indicating whether the node is a broker or not (True if the node is broker,
 * False if the node is a client.
 */
public final class NodeReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final String ipAddress;
    private final int publicPort;
    private final String nodeName;
    private final boolean isBroker;

    /**
     *
     */
    public NodeReference(String ipAddress, int publicPort, String nodeName,
                         boolean isBroker) {
        this.ipAddress = ipAddress;
        this.publicPort = publicPort;
        this.nodeName = nodeName;
        this.isBroker = isBroker;
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

    public String ipAddress() {
        return ipAddress;
    }

    public int publicPort() {
        return publicPort;
    }

    public String nodeName() {
        return nodeName;
    }

    public boolean isBroker() {
        return isBroker;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (NodeReference) obj;
        return Objects.equals(this.ipAddress, that.ipAddress) &&
                this.publicPort == that.publicPort &&
                Objects.equals(this.nodeName, that.nodeName) &&
                this.isBroker == that.isBroker;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, publicPort, nodeName, isBroker);
    }

}
