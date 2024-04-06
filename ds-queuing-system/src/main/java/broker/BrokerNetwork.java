package broker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * This class handles the aspects related to the network interface of a single broker.
 * More in detail, every broker is connected first to the locator via a TCP connection, then it is connected
 * to every other broker already instantiated as a "client" and keeps listening to a port for new brokers,
 * acting as a "server".
 */
public class BrokerNetwork implements Runnable {
    private final String locatorIPAddress;
    private final int locatorPort;

    private Socket socketToLocator; /** Socket for the connection with the locator. */
    private ObjectInputStream locatorSocketIS;  /** Input stream for the socket to the locator. */
    private ObjectOutputStream locatorSocketOS; /** Output stream for the socket to the locator. */

    private BrokerController brokerController;

    public BrokerNetwork(BrokerController controller, String locatorIPAddress, int locatorPort) {
        this.brokerController = controller;
        this.locatorIPAddress = locatorIPAddress;
        this.locatorPort = locatorPort;

        try {
            socketToLocator = new Socket();
            socketToLocator.connect(new InetSocketAddress(locatorIPAddress, locatorPort));
            locatorSocketIS = new ObjectInputStream(socketToLocator.getInputStream());
            locatorSocketOS = new ObjectOutputStream(socketToLocator.getOutputStream());
        } catch (IOException e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("Running thread...");
    }
}
