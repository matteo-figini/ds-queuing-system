package broker;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class represents the socket that a broker has opened with another broker already connected.
 * The first broker acts as a client.
 */
public class OtherBrokerSocket {
    // Application attributes
    private final String otherBrokerName;
    private final ExecutorService readFromBrokerService = Executors.newSingleThreadExecutor();
    // Network attributes
    private final Socket socket;
    private final ObjectInputStream otherBrokerIS;
    private final ObjectOutputStream otherBrokerOS;
    private final BrokerNetwork brokerNetworkRef;

    /**
     * Create the {@code OtherBrokerSocket} as a connection from the broker to another broker.
     * @param name Name of the other broker.
     * @param ipAddress IP address of the other broker.
     * @param port Public port on which the other broker keeps listening for new connections.
     * @param brokerNetworkRef Reference to the actual {@code BrokerNetwork}.
     */
    public OtherBrokerSocket(String name, String ipAddress, int port, BrokerNetwork brokerNetworkRef) throws IOException {
        this.otherBrokerName = name;
        this.brokerNetworkRef = brokerNetworkRef;

        this.socket = new Socket(ipAddress, port);
        this.otherBrokerIS = new ObjectInputStream(socket.getInputStream());
        this.otherBrokerOS = new ObjectOutputStream(socket.getOutputStream());
        this.readMessageFromOtherBroker();
    }

    /**
     * Send the {@code Message} passed as a parameter to the other broker.
     * @param message Message to be sent to the receiver.
     */
    public void sendMessage(Message message) {
        try {
            this.otherBrokerOS.writeObject(message);
            this.otherBrokerOS.reset();
        } catch (IOException e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Starts and execute the routine that keeps listening on the {@code InputStream} from the other broker.
     */
    public void readMessageFromOtherBroker() {
        readFromBrokerService.execute(() -> {
            while (!readFromBrokerService.isShutdown()) {
                Message message;
                try {
                    message = (Message) otherBrokerIS.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    message = null;
                    disconnect();
                }
                brokerNetworkRef.onMessageReceived(message, otherBrokerName);
            }
        });
    }

    /**
     * Disconnect the broker from the other broker, closing the socket and stopping the reading service.
     */
    public void disconnect () {
        if (!readFromBrokerService.isShutdown()) readFromBrokerService.shutdownNow();
        try {
            if (!socket.isClosed()) socket.close();
            brokerNetworkRef.onNodeServerDisconnection(this);
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to disconnect from " + otherBrokerName + ": " + e.getMessage());
        }
    }

    /**
     * @return The name of the connected node on which {@code OtherBrokerSocket} refers to.
     */
    public String getOtherBrokerName() {
        return otherBrokerName;
    }
}
