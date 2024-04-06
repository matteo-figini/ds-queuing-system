package locator;

import java.util.ArrayList;
import java.util.List;

public class LocatorController {
    /** Reference to the {@code LocatorNetwork} element. */
    private LocatorNetwork locatorNetworkRef;

    /** List containing a reference for each broker in the network. */
    private List<BrokerReference> brokersConnected;

    /** Number of brokers allowed in the network. This is assumed to be fixed during all the execution. */
    private final int maximumBrokerNumber;

    public LocatorController (int brokers) {
        brokersConnected = new ArrayList<>();
        this.maximumBrokerNumber = brokers;
    }

    public void setLocatorNetwork (LocatorNetwork locatorNetworkRef) {
        this.locatorNetworkRef = locatorNetworkRef;
    }
}
