package client;

import messages.Message;

/**
 * This class acts as an interface between the communication layer and the {@code ClientController}.
 */
public class ClientNetwork {
    private final ClientController clientController;
    private LocatorSocket socketToLocator;

    /**
     * Create the instance of {@code ClientNetwork} and tries to connect to the locator.
     * @param clientController Reference to the specific {@code ClientController}.
     * @param locatorIPAddress Locator's IP Address.
     * @param locatorPort Locator's port.
     */
    public ClientNetwork (ClientController clientController, String locatorIPAddress, int locatorPort) {
        this.clientController = clientController;
        socketToLocator = new LocatorSocket(this, locatorIPAddress, locatorPort);
    }

    /**
     * Starts and execute the routine that keeps listening to new incoming messages from the locator.
     */
    public void readMessagesFromLocator () {
        socketToLocator.readMessage();
    }

    /**
     * Send a message to the specified receiver.
     * @param receiver Receiver of the message. Allowed values for this field are:
     *                 - "locator": the message will be sent to the locator;
     *                 - "leader": the message will be sent to the leader of the brokers' network.
     * @param message Message to be sent to the receiver.
     */
    public void sendMessage(String receiver, Message message) {
        if (receiver.equalsIgnoreCase("locator")) {
            socketToLocator.sendMessage(message);
        }
    }

    /**
     * Forward a received message to the controller.
     * @param message Message received to be forwarded to the controller.
     * @param sender Sender of the message.
     */
    public void onMessageReceived(Message message, String sender) {
        clientController.update(message, sender);
    }
}
