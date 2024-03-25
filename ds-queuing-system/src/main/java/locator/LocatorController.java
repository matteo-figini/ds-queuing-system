package locator;

import java.util.ArrayList;
import java.util.List;

public class LocatorController {
    private LocatorNetwork locatorNetworkRef;
    private List<BrokerReference> brokersConnected;

    public LocatorController () {
        brokersConnected = new ArrayList<>();
    }

    public void setLocatorNetwork (LocatorNetwork locatorNetworkRef) {
        this.locatorNetworkRef = locatorNetworkRef;
    }


}
