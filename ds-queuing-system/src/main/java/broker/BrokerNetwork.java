package broker;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class handles the aspects related to the network interface of a single broker.
 * More in detail, every broker is connected first to the locator via a TCP connection, then it is connected
 * to every other broker already instantiated as a "client" and keeps listening to a port for new brokers,
 * acting as a "server".
 */
public class BrokerNetwork {
    // Locator's connection
    private Socket socketToLocator;
    private ObjectInputStream locatorSocketIS;
    private ObjectOutputStream locatorSocketOS;
    private final ExecutorService readFromLocatorService = Executors.newSingleThreadExecutor();

    // Other connections
    private BrokerServerSocket brokerServerSocket;

    // Reference to the broker's controller.
    private final BrokerController brokerController;

    public BrokerNetwork(BrokerController controller, String locatorIPAddress, int locatorPort) {
        this.brokerController = controller;
        try {
            socketToLocator = new Socket();
            socketToLocator.connect(new InetSocketAddress(locatorIPAddress, locatorPort));
            locatorSocketIS = new ObjectInputStream(socketToLocator.getInputStream());
            locatorSocketOS = new ObjectOutputStream(socketToLocator.getOutputStream());
            startBrokerServerSocket();
        } catch (IOException e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
        }
    }

    /**
     * Sends the message specified as parameter to the locator.
     * If an {@code IOException} occurs, the broker will be disconnected from the server.
     * @param message The message to be sent.
     */
    public void sendMessageToLocator (Message message) {
        try {
            this.locatorSocketOS.writeObject(message);
            this.locatorSocketOS.reset();
        } catch (IOException e) {
            e.printStackTrace(); // TODO: to be replaced with the effective disconnection.
        }
    }

    /**
     * Starts and execute the routine that keeps listening to new incoming messages from the locator.
     */
    public void readMessageFromLocator () {
        readFromLocatorService.execute(() -> {
            while (!readFromLocatorService.isShutdown()) {
                Message message;
                try {
                    message = (Message) locatorSocketIS.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    // TODO: handle disconnection from the locator
                    message = null;
                    readFromLocatorService.shutdownNow();
                }
                brokerController.update(message);
            }
        });
    }

    /**
     * Ask the user the public port on which the broker's server socket will run and instantiate the
     * {@code BrokerServerSocket}.
     */
    private void startBrokerServerSocket() {
        System.out.print("Insert the public port on which the broker will listen to new connections: ");
        int publicPort = Integer.parseInt(new Scanner(System.in).nextLine());
        this.brokerServerSocket = new BrokerServerSocket(publicPort, this);
        Thread thread = new Thread(brokerServerSocket);
        thread.start();
    }

    /**
     * @return The broker's public port.
     */
    public int getBrokerPublicPort () {
        return brokerServerSocket.getPublicPort();
    }
}
