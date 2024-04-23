package broker;

import misc.NodeReference;
import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
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

    // Other connections on which the broker acts as a server
    private BrokerServerSocket brokerServerSocket;
    private final HashMap<String, OtherNodeClientHandler> otherNodeClientHandlers = new HashMap<>();

    // Other connections on which the broker acts as a client
    private final HashMap<String, OtherBrokerSocket> otherBrokerSocketHashMap = new HashMap<>();

    // Reference to the broker's controller.
    private final BrokerController brokerController;

    /**
     * Create the {@code BrokerNetwork}.
     * @param controller {@code BrokerController} of the broker - must be already instantiated.
     * @param locatorIPAddress IP address of the remote locator.
     * @param locatorPort Port, on which the locator is listening for new connections.
     */
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
            disconnectFromLocator();
            System.out.println("[EXCEPTION] Cannot send the message to the locator. Disconnected.");
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
                    disconnectFromLocator();
                    message = null;
                    readFromLocatorService.shutdownNow();
                }
                brokerController.update(message);
            }
        });
    }

    /**
     * Disconnect the broker from the locator, closing the connection and stops listening for new messages.
     */
    public void disconnectFromLocator () {
        try {
            if (!socketToLocator.isClosed()) {
                socketToLocator.close();
            }
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to close the connection to the locator properly.");
            e.printStackTrace();
        }
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

    public void onMessageReceived (Message message, OtherNodeClientHandler otherNodeClientHandler) {
        brokerController.update(message);
    }

    public void onBrokerDisconnection(OtherNodeClientHandler otherBrokerClientHandler) {
        // TODO: to be managed
    }

    /**
     * Connects to the other broker
     * @param nodeReference
     * @return
     */
    public boolean connectToOtherBroker (NodeReference nodeReference) {
        try {
            OtherBrokerSocket otherBrokerSocket = new OtherBrokerSocket(
                    nodeReference.getNodeName(),
                    nodeReference.getIpAddress(),
                    nodeReference.getPublicPort(), this);
            otherBrokerSocketHashMap.put(nodeReference.getNodeName(), otherBrokerSocket);
            System.out.println("[INFO] Successfully connected to other broker: " + nodeReference);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean sendMessageToBroker (String receiverBroker, Message message) {
        boolean messageSent = false;
        if (receiverBroker.equals("all")) {
            // Used to send a message to all the brokers

        } else {
            if (otherBrokerSocketHashMap.containsKey(receiverBroker)) {
                // The current broker acts as a "client" w.r.t. the other broker
                OtherBrokerSocket otherBrokerSocket = otherBrokerSocketHashMap.get(receiverBroker);
                otherBrokerSocket.sendMessage(message);
            } else {
                System.out.println("[ERROR] Cannot send the message; broker " + receiverBroker + " not found!");
            }
        }
        return messageSent;
    }
}
