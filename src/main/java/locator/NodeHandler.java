package locator;

import messages.Message;
import messages.MessageType;
import messages.network.HelloRequestMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

/**
 * This class represents the client handler from the point of view of the Locator.
 * Indeed, every node connecting to the locator (either a broker or a client) is seen
 * as a "client" - referring to the TCP terminology - from the point of view of the
 * locator.
 */
public class NodeHandler implements Runnable {
    // Application attributes
    private String nodeName;
    // Network attributes
    private final Socket clientSocket;
    private final LocatorNetwork locatorNetwork;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private final Object inputLockObject = new Object();
    private final Object outputLockObject = new Object();

    /**
     * This constructor creates a socket NodeHandler which will manage by thread every single client connection.
     * @param locatorNetwork The current instance of the locator's network.
     * @param clientSocket The current instance of the socket with the client.
     */
    public NodeHandler(LocatorNetwork locatorNetwork, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.locatorNetwork = locatorNetwork;

        try {
            this.outputStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
            this.inputStream = new ObjectInputStream(this.clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * This method runs the thread of the client handler.
     * The role of the thread is to continuous listening to the socket port when new messages arrive
     * and send them to the locator.
     */
    @Override
    public void run() {
        System.out.println("[INFO] Established connection with " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (inputLockObject) {
                Message message = null;
                try {
                    message = (Message) inputStream.readObject();
                    System.out.println("[INFO] Received message: " + message.toString());
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("[EXCEPTION] " + e.getMessage());
                    try {
                        clientSocket.close();
                    } catch (IOException ex) {
                        System.out.println("[EXCEPTION] Unable to close the socket: " + ex.getMessage());
                    }
                    Thread.currentThread().interrupt();
                } catch (ClassCastException cce) {
                    cce.printStackTrace();
                    System.out.println("[EXCEPTION] Message was: " + message);
                }
                // If the message is valid, handle it.
                if (message != null) {
                    locatorNetwork.onMessageReceived(message, this);
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
                System.out.println("[INFO] Sending message: " + message.toString() + ", to node: " + clientSocket.getInetAddress());
                outputStream.writeObject(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to send message: " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Disconnect the current {@code NodeHandler} from the connected node, close the socket and leave the control
     * to the {@code LocatorNetwork}.
     */
    public void disconnect () {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to close the socket: " + e.getMessage());
        }
        if (!Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
        locatorNetwork.onClientDisconnection(this);
    }

    /**
     * @return The name of the node connected to the locator via this {@code NodeHandler}.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Set the name of the node connected to the locator via this {@code NodeHandler}.
     * @param nodeName Name of the connected node.
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
