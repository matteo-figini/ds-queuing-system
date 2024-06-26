package client;

import messages.Message;
import messages.application.AppendQueueRequest;
import messages.application.CreateQueueRequest;
import messages.application.ReadQueueRequest;
import messages.network.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class handles the logic of the client of the network.
 */
public class ClientController {
    private final String clientName;
    private ClientNetwork clientNetwork;
    private final ExecutorService keyboardInputRoutine = Executors.newSingleThreadExecutor();

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
        clientNetwork.readMessagesFromLocator();
    }

    /**
     * Start the communication flow with the locator by sending the {@code HelloRequestMessage}.
     */
    public void startCommunicationGreetings () {
        String localIPAddress;
        try {
            localIPAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            localIPAddress = "127.0.0.1";
            System.out.println("[EXCEPTION] Unable to retrieve the local IP address: " + e.getMessage());
            System.out.println("[EXCEPTION] Adding the default address: " + localIPAddress);
        }
        // Start the thread that continuously waits for new inputs and process them as commands.
        keyboardInputRoutine.execute(() -> {
            Scanner scanner = new Scanner(System.in);
            while (!keyboardInputRoutine.isShutdown()) {
                String readString = scanner.nextLine();
                interpretCommand(readString);
            }
        });
        // Field "nodePublicPort" is not relevant
        HelloRequestMessage helloMessage = new HelloRequestMessage(localIPAddress, 0, clientName, false);
        clientNetwork.sendMessage("locator", helloMessage);
    }

    /**
     * Take an action based on the type of the received message from the {@code ClientNetwork}.
     * @param message Message received.
     * @param sender Sender of the message received.
     */
    public void update (Message message, String sender) {
        if (message != null) {
            switch (message.type) {
                case HELLO_RESPONSE -> onHelloResponseMessage((HelloResponseMessage) message, sender);
                case LEADER_DISCOVERY_RESPONSE -> onLeaderDiscoveryResponse((LeaderDiscoveryResponse) message, sender);
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
            final int waitingSeconds = 20;
            System.out.println("[INFO] No available leader now: retrying in " + waitingSeconds + " seconds...");
            ScheduledExecutorService retrySendingMessage = Executors.newSingleThreadScheduledExecutor();
            retrySendingMessage.schedule(() -> clientNetwork.sendMessage("locator", new LeaderDiscoveryRequest()), waitingSeconds, TimeUnit.SECONDS);
        } else {
            System.out.println("[INFO] Setting available leader: " + message.getLeaderReference().nodeName());
            clientNetwork.connectToBrokerLeader(message.getLeaderReference());
        }
    }

    /**
     * Receive in input a command from the standard input and process it, based on the format of the command.
     * The command is then processed, according to the specific type.
     * @param command The command received from the standard input.
     */
    private void interpretCommand (String command) {
        String[] commandParts = command.split(" ");
        if (commandParts.length < 1) {
            return;
        }
        if (commandParts[0].equalsIgnoreCase("create")) interpretCreateCommand(commandParts);
        else if (commandParts[0].equalsIgnoreCase("append")) interpretAppendCommand(commandParts);
        else if (commandParts[0].equalsIgnoreCase("read")) interpretReadCommand(commandParts);
        else System.out.println("[ERROR] Unknown command.");
    }

    /**
     * Format the command string to create a new queue in the following format:
     *      create <queue_name>
     * Then, create the {@code CreateQueueRequest} and send the message to the broker's leader.
     * @param commandParts Array containing the words of the command.
     */
    private void interpretCreateCommand (String[] commandParts) {
        if (commandParts.length < 2) return;
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(commandParts[1]);
        System.out.println(createQueueRequest);
        clientNetwork.sendMessage("leader", createQueueRequest);
    }

    /**
     * Format the command string to append some integers to a queue in the following format:
     *      append <queue_name> <elem1> <elem2> ...
     * Then, create the {@code AppendQueueRequest} and send the message to the broker's leader.
     * @param commandParts Array containing the words of the command.
     */
    private void interpretAppendCommand (String[] commandParts) {
        if (commandParts.length < 2) return;
        List<Integer> elementsToAppend = IntStream.range(2, commandParts.length).mapToObj(i -> Integer.parseInt(commandParts[i])).collect(Collectors.toList());
        AppendQueueRequest appendQueueRequest = new AppendQueueRequest(commandParts[1], elementsToAppend);
        System.out.println(appendQueueRequest);
        clientNetwork.sendMessage("leader", appendQueueRequest);
    }

    /**
     * Format the command string to read new elements in a queue in the following format:
     *      read <queue_name>
     * Then, create the {@code ReadQueueRequest} and send the message to the broker's leader.
     * @param commandParts Array containing the words of the command.
     */
    private void interpretReadCommand (String[] commandParts) {
        if (commandParts.length < 2) return;
        ReadQueueRequest readQueueRequest = new ReadQueueRequest(commandParts[1]);
        System.out.println(readQueueRequest);
        clientNetwork.sendMessage("leader", readQueueRequest);
    }

    /**
     * When the leader is disconnected, the client starts asking the locator for who is the new leader.
     */
    public void onLeaderDisconnection() {
        clientNetwork.sendMessage("locator", new LeaderDiscoveryRequest());
    }
}