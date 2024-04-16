package locator;

/**
 * This class represents a description of a node reference from the point of view of the locator.
 * Every connected node has its reference to the {@code NodeHandler} class, a {@code String} representing
 * the node name and a boolean flag indicating whether the node is a broker or not (True if the node is broker,
 * False if the node is a client.
 */
public class NodeReference {
    private NodeHandler nodeHandler;
    private String nodeName;
    private boolean isBroker;

    public NodeReference(NodeHandler nodeHandler, String nodeName, boolean isBroker) {
        this.nodeHandler = nodeHandler;
        this.nodeName = nodeName;
        this.isBroker = isBroker;
    }

    public NodeHandler getNodeHandler() {
        return nodeHandler;
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
                "nodeHandler=" + nodeHandler +
                ", nodeName='" + nodeName + '\'' +
                ", isBroker=" + isBroker +
                '}';
    }
}
