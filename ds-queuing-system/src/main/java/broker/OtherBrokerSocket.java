package broker;

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
    private final Socket socket;
    private final ObjectInputStream otherBrokerIS;
    private final ObjectOutputStream otherBrokerOS;
    private final BrokerNetwork brokerNetworkRef;

    private final ExecutorService readFromBrokerService = Executors.newSingleThreadExecutor();

    /**
     * Create the {@code OtherBrokerSocket} as a connection from the broker to another broker.
     * @param ipAddress IP address of the other broker.
     * @param port Public port on which the other broker keeps listening for new connections.
     * @param brokerNetworkRef Reference to the actual {@code BrokerNetwork}.
     */
    public OtherBrokerSocket(String ipAddress, int port, BrokerNetwork brokerNetworkRef) throws IOException {
        this.brokerNetworkRef = brokerNetworkRef;
        this.socket = new Socket(ipAddress, port);
        this.otherBrokerIS = new ObjectInputStream(socket.getInputStream());
        this.otherBrokerOS = new ObjectOutputStream(socket.getOutputStream());
    }



}
