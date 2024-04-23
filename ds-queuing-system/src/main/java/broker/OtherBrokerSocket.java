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
    }

    /**
     * // TODO: doc
     * @param message
     */
    public void sendMessage(Message message) {
        try {
            this.otherBrokerOS.writeObject(message);
            this.otherBrokerOS.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessageFromOtherBroker() {
        readFromBrokerService.execute(() -> {
            while (!readFromBrokerService.isShutdown()) {
                Message message;
                try {
                    message = (Message) otherBrokerIS.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    // disconnectFromLocator();
                    message = null;
                    readFromBrokerService.shutdownNow();
                }
                // Handle message
            }
        });
    }
}
