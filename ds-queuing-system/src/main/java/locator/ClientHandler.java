package locator;

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
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final LocatorNetwork locatorNetwork;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private boolean isConnected = true;
    private final Object inputLockObject;
    private final Object outputLockObject;

    /**
     * This constructor creates a socket ClientHandler which will manage by thread every single client connection.
     * @param locatorNetwork The current instance of the locator's network.
     * @param clientSocket The current instance of the socket with the client.
     */
    public ClientHandler(LocatorNetwork locatorNetwork, Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.locatorNetwork = locatorNetwork;
        this.isConnected = true;
        this.inputLockObject = new Object();
        this.outputLockObject = new Object();

        try {
            this.outputStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
            this.inputStream = new ObjectInputStream(this.clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        System.out.println("[INFO] Established connection with " + clientSocket.getInetAddress().toString());

        while (!Thread.currentThread().isInterrupted()) {

        }

    }
}
