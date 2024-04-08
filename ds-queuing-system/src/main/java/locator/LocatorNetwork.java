package locator;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class handles all the network communication by the locator component.
 * The communication between the locator and the brokers is mediated via TCP protocol,
 * ensuring persistent connection.
 */
public class LocatorNetwork implements Runnable {
    /** Port on which the locator has the {@code ServerSocket} open. */
    private final int port;

    /** {@code ServerSocket} on which the locator is listening for new incoming connections. */
    private ServerSocket serverSocket;

    /** Reference to the {@code LocatorController}. */
    private final LocatorController locatorController;

    /**
     * Set the default parameters needed for running the locator.
     * @param locatorController Reference to the {@code LocatorController} - it must be already instantiated.
     * @param port Port on which the {@code ServerSocket} will be open.
     */
    public LocatorNetwork (LocatorController locatorController, int port) {
        this.locatorController = locatorController;
        this.port = port;
    }

    /**
     * Creates a thread that constantly listens to on the {@code ServerSocket}.
     * When a new (client or) broker asks for the connection, a new {@code ClientHandler} is created.
     */
    @Override
    public void run() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            System.out.println("[INFO] Locator's network running on " + Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port + " via TCP socket connection.");
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to start the locator's server socket.");
            System.out.println(e.getMessage());
        }

        // Keeps listening on the ServerSocket.
        // Every time a new node connects to the ServerSocket,
        // instantiate and run the corresponding ClientHandler.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[INFO] New connection request from: " + clientSocket.getInetAddress() +
                        " on port " + clientSocket.getPort());
                ClientHandler clientHandler = new ClientHandler(this, clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            } catch (IOException e) {
                System.out.println("[EXCEPTION] " + e.getMessage());
            }
        }
    }



    // TODO: add methods for adding the new client, to handle the client disconnection and the arrive of a message.
}
