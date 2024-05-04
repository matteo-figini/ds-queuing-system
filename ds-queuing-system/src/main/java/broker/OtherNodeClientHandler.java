package broker;

import messages.Message;
import messages.MessageType;
import messages.network.HelloRequestMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This class represents the connection from a broker A to another broker B on which the broker A acts as a server
 * and the broker B acts as a client, seen from the point of view of the broker A. The behavior is similar to a
 * generic client handler from the server side.
 */
public class OtherNodeClientHandler implements Runnable {
    // Application attributes
    private String otherNodeName;
    private final BrokerNetwork brokerNetworkRef;
    // Network attributes
    private final Socket brokerSocket;
    private ObjectInputStream brokerSocketIS;
    private ObjectOutputStream brokerSocketOS;
    private final Object inputLockObject = new Object();
    private final Object outputLockObject = new Object();

    /**
     * Create the {@code OtherNodeClientHandler} and open the corresponding streams.
     * @param brokerNetworkRef Reference to the {@code BrokerNetwork} class.
     * @param brokerSocket {@code Socket} open by the {@code BrokerServerSocket}.
     */
    public OtherNodeClientHandler(BrokerNetwork brokerNetworkRef, Socket brokerSocket) {
        this.brokerNetworkRef = brokerNetworkRef;
        this.brokerSocket = brokerSocket;

        // Open the streams on the socket
        try {
            this.brokerSocketOS = new ObjectOutputStream(brokerSocket.getOutputStream());
            this.brokerSocketIS = new ObjectInputStream(brokerSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("[EXCEPTION] " + e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("[INFO] Established a new connection with " + brokerSocket.getInetAddress());
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (inputLockObject) {
                Message message = null;
                try {
                    message = (Message) brokerSocketIS.readObject();
                    System.out.println("[INFO] Received message: " + message);
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("[EXCEPTION] " + e.getMessage());
                    try {
                        brokerSocket.close();
                    } catch (IOException ex) {
                        System.out.println("[EXCEPTION] Unable to close the socket: " + ex.getMessage());
                    }
                    Thread.currentThread().interrupt();
                }
                if (message != null) {
                    if (message.type == MessageType.HELLO_REQUEST) {
                        // Message received from another broker to establish a connection.
                        HelloRequestMessage helloRequestMessage = (HelloRequestMessage) message;
                        addNode(helloRequestMessage);
                    } else {
                        // Normal message to be handled.
                        brokerNetworkRef.onMessageReceived(message, otherNodeName);
                    }
                }
            }
        }
        disconnect();
    }

    /**
     * Sends a message to the client on the {@code ObjectOutputStream}.
     * @param message The message to send to the client.
     */
    public void sendMessage (Message message) {
        try {
            synchronized (outputLockObject) {
                brokerSocketOS.writeObject(message);
                brokerSocketOS.reset();
                System.out.println("Message sent: " + message.toString());
            }
        } catch (IOException e) {
            System.err.println("I/O Error.");
            disconnect();
        }
    }

    /**
     * Disconnect the current {@code OtherNodeClientHandler} from the connected node, close the socket
     * and leave the control to the {@code BrokerNetwork}.
     */
    public void disconnect () {
        try {
            if (!brokerSocket.isClosed()) {
                brokerSocket.close();
            }
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to close the BrokerSocket: " + e.getMessage());
        }
        if (!Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
        brokerNetworkRef.onBrokerDisconnection(this);
    }

    /**
     * Set the name of the node and pass the control to {@code BrokerNetwork} to properly handle the addition of a new
     * connected node.
     * @param message "Hello" message received from the other node.
     */
    public void addNode (HelloRequestMessage message) {
        this.otherNodeName = message.getNodeName();
        brokerNetworkRef.addNode(message, this);
    }

    /**
     * @return The name of the other node connected via this {@code OtherNodeClientHandler}.
     */
    public String getOtherNodeName() {
        return otherNodeName;
    }
}
