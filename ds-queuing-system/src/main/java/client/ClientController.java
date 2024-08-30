package client;

import messages.Message;
import messages.MessageType;
import messages.application.*;
import messages.network.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the logic of the client of the network.
 */
public class ClientController {
    private final String clientName;
    private ClientNetwork clientNetwork;
    private final ExecutorService keyboardInputRoutine = Executors.newSingleThreadExecutor();
    private CommandInterpreter commandInterpreter;

    private String localIPAddress;

    /**
     * True if at the moment there is a connection with the leader.
     *
     * In case the network's leader is missing it is not possible to send
     * a new request.
     */
    private boolean connectedToLeader = false;
    private final Object mutexConnectedToLeader = new Object();

    /**
     * True if I've already sent an operation request, and I'm
     * waiting for a response.
     *
     * In that case it's not possible to send a new command
     * until we've received a response for the current one.
     */
    private boolean waitingForOperationResponse = false;
    private final Object mutexWaitingForOperationResponse = new Object();

    /**
     * Copy of the last request sent to the leader of the network.
     * Saved in case a OperationStatusRequest has to be sent.
     */
    private OperationRequest lastRequest;

    /**
     * Create the instance of {@code ClientController}.
     * @param clientName Name of the client.
     */
    public ClientController (String clientName) {
        this.clientName = clientName;
    }

    /**
     * Set the reference to the {@code ClientNetwork} passed as parameter and starts listening to new messages
     * from the locator.
     * @param clientNetwork Reference of the {@code ClientNetwork}.
     */
    public void setClientNetwork(ClientNetwork clientNetwork) {
        this.clientNetwork = clientNetwork;
        this.commandInterpreter = new CommandInterpreter(this, clientName);
        clientNetwork.readMessagesFromLocator();
    }

    /**
     * Start the communication flow with the locator by sending the {@code HelloRequestMessage}.
     */
    public void startCommunicationGreetings () {
        try {
            this.localIPAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            this.localIPAddress = "127.0.0.1";
            System.out.println("[EXCEPTION] Unable to retrieve the local IP address: " + e.getMessage());
            System.out.println("[EXCEPTION] Adding the default address: " + localIPAddress);
        }
        // Start the thread that continuously waits for new inputs and process them as commands.
        keyboardInputRoutine.execute(this::inputRoutine);
        // Field "nodePublicPort" is not relevant
        HelloRequestMessage helloMessage = new HelloRequestMessage(this.localIPAddress, 0, clientName, false);
        clientNetwork.sendMessage("locator", helloMessage);
    }

    /**
     * Code of the thread that continuously waits for new inputs
     * and process them as commands.
     */
    private void inputRoutine()
    {
        Scanner scanner = new Scanner(System.in);
        while (!keyboardInputRoutine.isShutdown()) {
            String readString = scanner.nextLine();

            synchronized (mutexConnectedToLeader)
            {
                synchronized (mutexWaitingForOperationResponse)
                {
                    if(connectedToLeader && !waitingForOperationResponse)
                    {
                        commandInterpreter.interpretAndSendCommand(readString);
                    }
                    else if(!connectedToLeader)
                    {
                        System.out.println("[ERROR] Cannot make requests while not connected to the leader.");
                    }
                    else if(waitingForOperationResponse)
                    {
                        System.out.println("[ERROR] Cannot make new requests while waiting for an operation to complete.");
                    }
                }
            }
        }
    }

    /**
     * Send a message to the specified receiver.
     * @param receiver Name of the receiver.
     * @param message {@code Message} to be sent.
     */
    public void sendMessage (String receiver, Message message) {

        if(message.type == MessageType.APPEND_QUEUE_REQUEST || message.type == MessageType.CREATE_QUEUE_REQUEST ||
           message.type == MessageType.READ_QUEUE_REQUEST)
        {
            synchronized (mutexWaitingForOperationResponse)
            {
                waitingForOperationResponse = true;
            }

            // Save the last request
            lastRequest = (OperationRequest) message;
        }

        clientNetwork.sendMessage(receiver, message);
    }

    /**
     * Take an action based on the type of the received message from the {@code ClientNetwork}.
     * @param message Message received.
     * @param sender Sender of the message received.
     */
    public void update (Message message, String sender) {
        if (message != null) {

            if(message.type == MessageType.APPEND_QUEUE_RESPONSE || message.type == MessageType.CREATE_QUEUE_RESPONSE ||
                    message.type == MessageType.READ_QUEUE_RESPONSE)
            {
                synchronized (mutexWaitingForOperationResponse)
                {
                    waitingForOperationResponse = false;
                }
            }

            switch (message.type) {
                case HELLO_RESPONSE -> onHelloResponseMessage((HelloResponseMessage) message, sender);
                case LEADER_DISCOVERY_RESPONSE -> onLeaderDiscoveryResponse((LeaderDiscoveryResponse) message, sender);
                case CREATE_QUEUE_RESPONSE -> onCreateQueueResponse((CreateQueueResponse) message);
                case READ_QUEUE_RESPONSE -> onReadQueueResponse((ReadQueueResponse) message);
                case APPEND_QUEUE_RESPONSE -> onAppendQueueResponse((AppendQueueResponse) message);
                case OPERATION_STATUS_RESPONSE -> onOperationStatusResponse((OperationStatusResponse) message);
                default -> System.out.println("[EXCEPTION] Unhandled message type: " + message.type);
            }
        }
    }

    /**
     * Handles the receiving of a {@code HelloResponseMessage}.
     * @param message Message received.
     * @param sender Sender of the message (the locator should be the sender of the message).
     */
    private void onHelloResponseMessage (HelloResponseMessage message, String sender) {
        if (message.isConnectionAccepted()) {
            System.out.println("[INFO] From " + sender + ": connection to the locator accepted.");
            clientNetwork.sendMessage("locator", new LeaderDiscoveryRequest());
        } else {
            System.out.println("[ERROR] Cannot connect as a client to the locator.");
            System.exit(0);
        }
    }

    /**
     * Handles the receiving of a {@code LeaderDiscoveryResponse} message.
     * @param message Message received.
     * @param sender Sender of the message (the locator should be the sender of the message).
     */
    private void onLeaderDiscoveryResponse (LeaderDiscoveryResponse message, String sender) {
        if (message.absenceOfLeader()) {

            synchronized (mutexConnectedToLeader)
            {
                connectedToLeader = false;
            }

            final int waitingSeconds = 5;
            System.out.println("[INFO] No available leader now: retrying in " + waitingSeconds + " seconds...");
            ScheduledExecutorService retrySendingMessage = Executors.newSingleThreadScheduledExecutor();
            retrySendingMessage.schedule(() -> clientNetwork.sendMessage("locator", new LeaderDiscoveryRequest()), waitingSeconds, TimeUnit.SECONDS);
        } else {
            System.out.println("[INFO] Setting available leader: " + message.getLeaderReference().nodeName());
            HelloRequestMessage helloMessage = new HelloRequestMessage(this.localIPAddress, 0, clientName, false);
            clientNetwork.connectToBrokerLeader(message.getLeaderReference(), helloMessage);

            synchronized (mutexConnectedToLeader)
            {
                connectedToLeader = true;
            }

            if(shouldAskOperationStatus())
            {
                askOperationStatus();
            }
        }
    }

    /**
     * @return True if there is an operation pending and the
     * client should ask the leader (of the raft network) info
     * about its operation.
     */
    private boolean shouldAskOperationStatus()
    {
        boolean shouldAsk = false;
        synchronized (mutexWaitingForOperationResponse)
        {
            if(waitingForOperationResponse)
            {
                shouldAsk = waitingForOperationResponse;
            }
        }

        return shouldAsk;
    }

    /**
     * Called in case we are waiting for an operation response,
     * the leader has crashed (disconnected) and a new leader has
     * spawn. Send an OperationStatusRequest.
     */
    private void askOperationStatus()
    {
        final OperationStatusRequest request = new OperationStatusRequest(lastRequest);
        sendMessage("leader", request);
    }

    private void onOperationStatusResponse(final OperationStatusResponse response)
    {
        System.out.println("[INFO] Operation status response: " + response.getInfoMessage());

        if(response.getInfoMessage().equals(OperationStatusResponse.OPERATION_COMMITTED))
        {
            // Probably the original response was lost
            // We are not waiting anymore
            synchronized (mutexWaitingForOperationResponse)
            {
                waitingForOperationResponse = false;
            }
        }
    }

    /**
     * When the leader is disconnected, the client starts asking the locator for who is the new leader.
     * The request is postponed with a negligible delay to allow the locator to update and send stable information.
     */
    public void onLeaderDisconnection() {

        synchronized (mutexConnectedToLeader)
        {
            connectedToLeader = false;
        }

        ScheduledExecutorService newLeaderRequest = Executors.newSingleThreadScheduledExecutor();
        newLeaderRequest.schedule(() -> clientNetwork.sendMessage("locator", new LeaderDiscoveryRequest()),
                1, TimeUnit.SECONDS);
    }

    private void onCreateQueueResponse(final CreateQueueResponse response)
    {
        if(response.getStatus())
        {
           System.out.println("[INFO] Queue correctly created");
        }
        else
        {
            System.out.println("[ERROR] An error occurred while attempting to create the queue: " +
                    response.getInfoMessage());
        }
    }

    private void onAppendQueueResponse(final AppendQueueResponse response)
    {
        if(response.getStatus())
        {
            System.out.println("[INFO] Append correctly executed");
        }
        else
        {
            System.out.println("[ERROR] An error occurred while attempting to append to the queue: " +
                    response.getInfoMessage());
        }
    }

    private void onReadQueueResponse(final ReadQueueResponse response)
    {
        if(response.getStatus())
        {
            System.out.println("[INFO] Value from the queue: " + response.getElementsRead());
        }
        else
        {
            System.out.println("[ERROR] An error occurred while attempting to read from the queue: " +
                    response.getInfoMessage());
        }
    }
}