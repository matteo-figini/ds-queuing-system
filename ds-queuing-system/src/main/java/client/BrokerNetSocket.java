package client;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class handles all the logic behind the connection to the broker's leader.
 * The client acts as a "client" for the broker leader.
 */
public class BrokerNetSocket {
    // Application attributes
    private final String leaderName;
    private final ExecutorService readFromLeaderService = Executors.newSingleThreadExecutor();
    // Network attributes
    private final Socket socket;
    private final ObjectInputStream objectInputStream;
    private final ObjectOutputStream objectOutputStream;
    private final ClientNetwork clientNetwork;

    /**
     * Instantiate the Socket connection to the broker's leader.
     * @param leaderName Name of the broker's leader.
     * @param ipAddress IP Address of the broker's leader.
     * @param port Public port on which the broker's leader is listening for new connections.
     * @param clientNetwork Reference to the {@code ClientNetwork} of this client.
     */
    public BrokerNetSocket (String leaderName, String ipAddress, int port, ClientNetwork clientNetwork) throws IOException {
        this.leaderName = leaderName;
        this.clientNetwork = clientNetwork;

        this.socket = new Socket(ipAddress, port);
        this.objectInputStream = new ObjectInputStream(socket.getInputStream());
        this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        this.readMessageFromLeader();
    }

    /**
     * Send the {@code Message} passed as a parameter to the leader.
     * @param message Message to be sent to the receiver.
     */
    public void sendMessage(Message message) {
        try {
            this.objectOutputStream.writeObject(message);
            this.objectOutputStream.reset();
        } catch (IOException e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
            disconnect();
        }
    }

    /**
     * Starts and execute the routine that keeps listening on the {@code InputStream} from the leader.
     */
    public void readMessageFromLeader() {
        readFromLeaderService.execute(() -> {
            while (!readFromLeaderService.isShutdown()) {
                Message message;
                try {
                    message = (Message) objectInputStream.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    message = null;
                    disconnect();
                }
                clientNetwork.onMessageReceived(message, leaderName);
            }
        });
    }

    /**
     * Disconnect the broker from the other broker, closing the socket and stopping the reading service.
     */
    public void disconnect () {
        if (!readFromLeaderService.isShutdown()) readFromLeaderService.shutdownNow();
        try {
            if (!socket.isClosed()) socket.close();
            // TODO: clientNetwork.onNodeServerDisconnection(this);
        } catch (IOException e) {
            System.out.println("[EXCEPTION] Unable to disconnect from " + leaderName + ": " + e.getMessage());
        }
    }

    /**
     * @return The name of the broker's leader.
     */
    public String getLeaderName() {
        return leaderName;
    }
}
