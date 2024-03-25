package locator;

import locator.LocatorController;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;

/**
 * This class handles all the network communication by the locator component.
 * The communication between the locator and the brokers is mediated via TCP protocol,
 * ensuring persistent connection.
 * TODO: how is mediated the connection between the locator and the client (if required)? TCP/UDP?
 */
public class LocatorNetwork implements Runnable {
    private final int port;
    private ServerSocket serverSocket;
    private LocatorController locatorController;

    public LocatorNetwork (LocatorController locatorController, int port) {
        this.locatorController = locatorController;
        this.port = port;
    }

    /**
     * Creates a thread that constantly listens to on the {@code ServerSocket}.
     * When a new (client or) broker asks for the connection, a new {@code } is created.
     */
    @Override
    public void run() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            System.out.println("Locator's network running on " + Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port + " via TCP socket connection.");
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to start the locator's server socket.");
            System.out.println(e.getMessage());
        }

        while (!Thread.currentThread().isInterrupted()) {
            // TODO: listen to new incoming connection
        }
    }
}
