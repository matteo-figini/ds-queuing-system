package broker;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class handles the communication layer between the broker and the locator from the broker's side.
 */
public class LocatorSocket {
    // Locator's connection
    private final Socket socketToLocator;
    private ObjectInputStream locatorSocketIS;
    private ObjectOutputStream locatorSocketOS;
    private final ExecutorService readFromLocatorService = Executors.newSingleThreadExecutor();

    // Reference to the Broker Network
    private final BrokerNetwork brokerNetwork;

    /**
     * Create the {@code LocatorSocket} in order to connect to the locator.
     * @param brokerNetwork Reference to the current {@code BrokerNetwork}.
     * @param locatorIP IP Address of the locator.
     * @param locatorPort Public port on which the locator is listening for new connections.
     */
    public LocatorSocket(BrokerNetwork brokerNetwork, String locatorIP, int locatorPort) {
        this.brokerNetwork = brokerNetwork;
        socketToLocator = new Socket();
        try {
            socketToLocator.connect(new InetSocketAddress(locatorIP, locatorPort));
            locatorSocketIS = new ObjectInputStream(socketToLocator.getInputStream());
            locatorSocketOS = new ObjectOutputStream(socketToLocator.getOutputStream());
        } catch (IOException e) {
            System.out.println("[EXCEPTION] " + e);
        }
    }

    /**
     * Send a message to the locator.
     * If the message cannot be sent, the broker will be disconnected from the locator
     * @param message Message to be sent.
     */
    public void sendMessage (Message message) {
        try {
            locatorSocketOS.writeObject(message);
            locatorSocketOS.reset();
        } catch (IOException e) {
            disconnect();
            System.out.println("[EXCEPTION] Cannot send the message to the locator. Disconnected.");
        }
    }

    /**
     * Starts and execute the routine that keeps listening to new messages from the locator.
     * When a new message is received, the message will be handled by {@code BrokerNetwork}.
     */
    public void readMessage () {
        readFromLocatorService.execute(() -> {
            while (!readFromLocatorService.isShutdown()) {
                Message message;
                try {
                    message = (Message) locatorSocketIS.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    disconnect();
                    message = null;
                    readFromLocatorService.shutdown();
                }
                brokerNetwork.onMessageReceived(message, "locator");
            }
        });
    }

    /**
     * Disconnect the broker from the locator, closing the connection and stops listening for new messages.
     */
    public void disconnect () {
        try {
            if (!socketToLocator.isClosed()) {
                socketToLocator.close();
            }
            brokerNetwork.onLocatorDisconnection(String.valueOf(socketToLocator.getInetAddress()), socketToLocator.getPort());
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to close the connection to the locator properly.");
        }
    }
}
