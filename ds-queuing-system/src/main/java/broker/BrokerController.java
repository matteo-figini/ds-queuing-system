package broker;

import messages.Message;
import messages.network.HelloRequestMessage;
import messages.network.HelloResponseMessage;
import messages.network.NetDiscoveryRequestMessage;
import messages.network.NetDiscoveryResponseMessage;

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * This class represents the main element of a broker, managing all the underlying logic
 * and acting as a mediator between the application layer and the network layer.
 */
public class BrokerController {
    private final String brokerName;
    private BrokerNetwork brokerNetwork;

    /**
     * Create the {@code BrokerController} instance.
     * @param brokerName Name of the broker.
     */
    public BrokerController (String brokerName) {
        this.brokerName = brokerName;
    }

    /**
     * Send a {@code HelloRequestMessage} to the locator.
     */
    public void startCommunicationGreetings () {
        String localIPAddress;
        try {
            localIPAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("[EXCEPTION] Unable to retrieve the local IP address: " + e.getMessage());
            System.out.println("[EXCEPTION] Adding the default address \"127.0.0.1\".");
            localIPAddress = "127.0.0.1";
        }
        int publicPort = brokerNetwork.getBrokerPublicPort();
        HelloRequestMessage helloMessage = new HelloRequestMessage(localIPAddress, publicPort, brokerName, true);
        brokerNetwork.sendMessageToLocator(helloMessage);
    }

    /**
     * Set the {@code BrokerNetwork} reference for that broker and run the routine that listen to incoming messages
     * from the locator.
     * @param brokerNetwork The {@code BrokerNetwork} reference.
     */
    public void setBrokerNetwork(BrokerNetwork brokerNetwork) {
        this.brokerNetwork = brokerNetwork;
        brokerNetwork.readMessageFromLocator();
    }

    /**
     * Receives a message from the {@code BrokerNetwork} and process it, based on the message type.
     * @param message The message received.
     */
    public void update (Message message) {
        if (message != null) {
            switch (message.type) {
                case HELLO_RESPONSE -> {
                    HelloResponseMessage helloResponseMessage = (HelloResponseMessage) message;
                    if (helloResponseMessage.isConnectionAccepted()) {
                        brokerNetwork.sendMessageToLocator(new NetDiscoveryRequestMessage());
                    }
                }
                case NET_DISCOVERY_RESPONSE -> {
                    NetDiscoveryResponseMessage netDiscoveryResponseMessage = (NetDiscoveryResponseMessage) message;
                    System.out.println(netDiscoveryResponseMessage);
                }
            }
        }
    }
}
