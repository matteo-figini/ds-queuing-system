package client;

import messages.Message;
import messages.network.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
                case HELLO_RESPONSE -> {
                    HelloResponseMessage helloResponseMessage = (HelloResponseMessage) message;
                    if (helloResponseMessage.isConnectionAccepted()) {
                        System.out.println("[INFO] Connection to the locator accepted.");
                        clientNetwork.sendMessage("locator", new LeaderDiscoveryRequest());
                    } else {
                        System.out.println("[ERROR] Cannot connect as a client to the locator.");
                        System.exit(0);
                    }
                }
                case LEADER_DISCOVERY_RESPONSE -> {
                    LeaderDiscoveryResponse leaderDiscoveryResponse = (LeaderDiscoveryResponse) message;
                    if (leaderDiscoveryResponse.absenceOfLeader()) {
                        final int waitingSeconds = 10;
                        System.out.println("[INFO] No available leader now: retrying in " + waitingSeconds + " seconds...");

                        ScheduledExecutorService retrySendingMessage = Executors.newSingleThreadScheduledExecutor();
                        retrySendingMessage.schedule(() -> clientNetwork.sendMessage("locator", new LeaderDiscoveryRequest()), waitingSeconds, TimeUnit.SECONDS);
                    } else {
                        System.out.println("[INFO] Setting available leader: " + leaderDiscoveryResponse.getLeaderReference().nodeName());
                        // TODO: instantiate the connection to the leader
                    }
                }
            }
        }
    }

    /**
     * Receive in input a command from the standard input and process it, based on the format of the command.
     * @param command The command received from the standard input.
     */
    private void interpretCommand (String command) {
        System.out.println(command);
    }
}
