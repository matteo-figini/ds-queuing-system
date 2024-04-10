package locator;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This class represents the client handler from the point of view of the Locator.
 * Indeed, every node connecting to the locator (either a broker or a client) is seen
 * as a "client" - referring to the TCP terminology - from the point of view of the
 * locator.
 */
public class NodeHandler implements Runnable {
    private final Socket clientSocket;
    private final LocatorNetwork locatorNetwork;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private final Object inputLockObject;
    private final Object outputLockObject;

    /**
     * This constructor creates a socket NodeHandler which will manage by thread every single client connection.
     * @param locatorNetwork The current instance of the locator's network.
     * @param clientSocket The current instance of the socket with the client.
     */
    public NodeHandler(LocatorNetwork locatorNetwork, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.locatorNetwork = locatorNetwork;
        this.inputLockObject = new Object();
        this.outputLockObject = new Object();

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
        System.out.println("[INFO] Established connection with " + clientSocket.getInetAddress());


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
                }

                if (message != null) {
                    locatorNetwork.onMessageReceived(message);
                    // TODO: handle the message to the locator.
                }
            }
        }
        // TODO: handle the client disconnection from the locator side
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to close the socket: " + e.getMessage());
        }
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
                outputStream.reset();
                System.out.println("[INFO] Message sent.");
            }
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to send message: " + message);
            // TODO: disconnect the node
        }
    }


}
