package locator;

import messages.Message;

import java.util.ArrayList;
import java.util.List;

public class LocatorController {
    /** Reference to the {@code LocatorNetwork} element. */
    private LocatorNetwork locatorNetworkRef;

    /** List containing a reference for each broker in the network. */
    private final List<NodeReference> nodesConnected;

    /** Number of brokers actually connected in the network. */
    private int numberOfConnectedBrokers = 0;

    /** Number of brokers allowed in the network. This is assumed to be fixed during all the execution. */
    private final int maximumBrokerNumber;

    public LocatorController (int brokers) {
        nodesConnected = new ArrayList<>();
        this.maximumBrokerNumber = brokers;
    }

    public void setLocatorNetwork (LocatorNetwork locatorNetworkRef) {
        this.locatorNetworkRef = locatorNetworkRef;
    }

    /**
     * Handles the receiving of a message.
     * @param message The message received.
     */
    public void onMessageReceived (Message message) {
        switch (message.type) {
            case HELLO_REQUEST -> System.out.println("Suca!");
        }
    }

    /* ---------- APPLICATION METHODS ---------- */
    /**
     * Adds a new {@code NodeReference} into the data structure keeping the state of the nodes.
     * @param nodeRef The {@code NodeReference} to be added.
     */
    public void addNewConnectedNode (NodeReference nodeRef) {
        System.out.println("[INFO] Adding new node: " + nodeRef);
        nodesConnected.add(nodeRef);
        System.out.println("[INFO] Node added.");
    }

    /* ---------- UTILITIES ---------- */

}
