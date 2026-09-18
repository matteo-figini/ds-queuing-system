package broker;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class represents the broker's server socket actively listening for new connection
 * from other brokers. The behavior is similar to a generic {@code ServerSocket} for a server.
 */
public class BrokerServerSocket implements Runnable {
    private ServerSocket brokerServerSocket;
    private final BrokerNetwork brokerNetworkRef;
    private final int publicPort;

    /**
     * Create the {@code BrokerServerSocket}.
     * @param publicPort Public port on which the broker keeps listening for new connections.
     * @param brokerNetworkRef Reference to the {@code BrokerNetwork}.
     */
    public BrokerServerSocket (int publicPort, BrokerNetwork brokerNetworkRef) {
        this.publicPort = publicPort;
        this.brokerNetworkRef = brokerNetworkRef;
    }

    @Override
    public void run() {
        try {
            this.brokerServerSocket = new ServerSocket(this.publicPort);
            System.out.println("[INFO] Broker on " + Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.publicPort + " ready to listen for incoming connection.");
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to start the broker's server socket.");
            System.out.println(e.getMessage());
        }

        // Keeps listening on the ServerSocket.
        // Every time a new node connects to the ServerSocket, instantiate and run the corresponding NodeHandler.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = brokerServerSocket.accept();
                System.out.println("[INFO] New connection request from: " + clientSocket.getInetAddress() +
                        " on port " + clientSocket.getPort());
                OtherNodeClientHandler nodeClientHandler = new OtherNodeClientHandler(brokerNetworkRef, clientSocket);
                Thread thread = new Thread(nodeClientHandler);
                thread.start();
            } catch (IOException e) {
                System.out.println("[EXCEPTION] " + e.getMessage());
            } catch (NullPointerException e) {
                System.out.println("[EXCEPTION] Port " + publicPort + " is already in use! The program will (hopefully, gracefully) end.");
                System.out.println("[INFO] Restart the program by choosing another port.");
                System.exit(1);
            }
        }
    }

    /**
     * @return The public broker's port on which {@code ServerSocket} keeps listening.
     */
    public int getPublicPort() {
        return publicPort;
    }
}
