package client;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class handles the communication between an external client and the locator.
 */
public class LocatorSocket {
    private Socket socketToLocator;
    private ObjectInputStream socketInputStream;
    private ObjectOutputStream socketOutputStream;
    private final ExecutorService readFromLocatorService = Executors.newSingleThreadExecutor();

    private final ClientNetwork clientNetwork;

    /**
     * Create the {@code LocatorSocket} object for the connection from the client to the locator.
     * @param clientNetwork Reference to the {@code ClientNetwork}.
     * @param locatorIP IP address of the locator.
     * @param locatorPort Port, on which the locator keeps listening for new connections.
     */
    public LocatorSocket(ClientNetwork clientNetwork, String locatorIP, int locatorPort) {
        this.clientNetwork = clientNetwork;
        socketToLocator = new Socket();
        try {
            socketToLocator.connect(new InetSocketAddress(locatorIP, locatorPort));
            socketInputStream = new ObjectInputStream(socketToLocator.getInputStream());
            socketOutputStream = new ObjectOutputStream(socketToLocator.getOutputStream());
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
            socketOutputStream.writeObject(message);
            socketOutputStream.reset();
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
                    message = (Message) socketInputStream.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    disconnect();
                    message = null;
                    readFromLocatorService.shutdown();
                }
                clientNetwork.onMessageReceived(message, "locator");
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
            // clientNetwork.onLocatorDisconnection(String.valueOf(socketToLocator.getInetAddress()), socketToLocator.getPort());
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to close the connection to the locator properly.");
        }
    }
}
