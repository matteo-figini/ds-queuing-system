package broker;

import messages.network.HelloRequestMessage;

/**
 * This class represents the main element of a broker, managing all the underlying logic
 * and acting as a mediator between the application layer and the network layer.
 */
public class BrokerController {

    /** Name of the broker. */
    private final String brokerName;

    /** Reference to the {@code BrokerNetwork}. */
    private BrokerNetwork brokerNetwork;

    public BrokerController(String brokerName) {
        this.brokerName = brokerName;
    }

    public void startCommunicationGreetings () {
        // Send hello message
        brokerNetwork.sendMessageToLocator(new HelloRequestMessage(brokerName, true));
    }


    public void setBrokerNetwork(BrokerNetwork brokerNetwork) {
        this.brokerNetwork = brokerNetwork;
    }


}
