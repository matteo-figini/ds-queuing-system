package broker;

import application.AppQueueManager;
import application.Operation;
import application.exceptions.EndOfQueueException;
import application.exceptions.NameAlreadyUsedException;
import application.exceptions.QueueNotFoundException;
import application.operations.CreateQueue;
import application.operations.ReadQueue;
import application.operations.AppendQueue;
import messages.MessageType;
import messages.application.*;
import messages.network.*;
import messages.raft.RaftAppendMessage;
import misc.NetworkState;
import misc.NodeReference;
import messages.Message;
import messages.network.HelloRequestMessage;
import messages.network.HelloResponseMessage;
import messages.network.NetDiscoveryRequestMessage;
import messages.network.NetDiscoveryResponseMessage;
import raft.RaftNode;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class represents the main element of a broker, managing all the underlying logic
 * and acting as a mediator between the application layer and the network layer.
 */
public class BrokerController {
    private final String brokerName;
    private final String localIPAddress;
    private BrokerNetwork brokerNetwork;

    // This structure keeps a reference to every other node (broker or client) connected to the broker, identified
    // by their name.
    private final HashMap<String, NodeReference> nodesConnected = new HashMap<>();
    private String leaderBroker;

    // Raft stuff
    private RaftNode raftNode;
    Thread raftThread;
    private LinkedBlockingQueue<Message> eventsQueue = new LinkedBlockingQueue<>();

    // Application stuff
    AppQueueManager queueManager = new AppQueueManager();

    /**
     * Create the {@code BrokerController} instance.
     * @param brokerName Name of the broker.
     */
    public BrokerController (String brokerName, String localIPAddress) {
        this.brokerName = brokerName;
        this.localIPAddress = localIPAddress;
        this.leaderBroker = null;
    }

    /**
     * Send a {@code HelloRequestMessage} to the locator.
     */
    public void startCommunicationGreetings () {
        int publicPort = brokerNetwork.getBrokerPublicPort();
        HelloRequestMessage helloMessage = new HelloRequestMessage(this.localIPAddress, publicPort, this.brokerName, true);
        brokerNetwork.sendMessage("locator", helloMessage);
    }

    /**
     * Set the {@code BrokerNetwork} reference for that broker and run the routine that listen to incoming messages
     * from the locator.
     * @param brokerNetwork The {@code BrokerNetwork} reference.
     */
    public void setBrokerNetwork(BrokerNetwork brokerNetwork) {
        this.brokerNetwork = brokerNetwork;
        brokerNetwork.readMessagesFromLocator();
    }

    /**
     * Send a message to the corresponding receiver.
     * @param receiver Name of the receiver.
     * @param message {@code Message} to be sent.
     */
    public void sendMessage (String receiver, Message message) {
        brokerNetwork.sendMessage(receiver, message);
    }

    /**
     * Receives a message from the {@code BrokerNetwork} and process it, based on the message type.
     * If the message type is not supported, an error message will be printed on the screen.
     * @param message The message received.
     */
    public void update (Message message, String sender) {
        if (message != null) {
            switch (message.type) {
                case HELLO_RESPONSE -> onHelloResponseMessage((HelloResponseMessage) message);
                case NET_DISCOVERY_RESPONSE -> onNetDiscoveryResponseMessage((NetDiscoveryResponseMessage) message);
                case BROKERS_READY_MESSAGE -> onBrokersReadyMessage((BrokersReadyMessage) message);
                case VOTE_REQUEST, VOTE_RESPONSE, LOG_REQUEST, LOG_RESPONSE, ELECTION_OUT_OF_TIME_CANDIDATE, ELECTION_OUT_OF_TIME_FOLLOWER,
                     LEADER_DISCONNECTED, START_ELECTION, ASK_LEADER_REQUEST, ASK_LEADER_RESPONSE -> {
                    // Raft messages
                    eventsQueue.add(message);
                }
                case CREATE_QUEUE_REQUEST, APPEND_QUEUE_REQUEST, READ_QUEUE_REQUEST -> onClientRequest(message);

                default -> System.out.println("[ERROR] Unknown message type " + message.type);
            }
        }
    }

    /**
     * Handle a message of type {@code HelloResponseMessage}.
     * @param message Message received.
     */
    private void onHelloResponseMessage (HelloResponseMessage message) {
        if (message.isConnectionAccepted()) {
            sendMessage("locator", new NetDiscoveryRequestMessage());
        } else {
            System.out.println("[ERROR] Cannot connect as a broker to the locator.");
            System.exit(0);
        }
    }

    /**
     * Handle a message of type {@code NetDiscoveryResponseMessage}.
     * @param message Message received.
     */
    private void onNetDiscoveryResponseMessage (NetDiscoveryResponseMessage message) {
        message.getBrokersConnected().stream().filter(nodeReference -> !nodeReference.nodeName().equals(brokerName))
                .forEach(nodeReference -> nodesConnected.put(nodeReference.nodeName(), nodeReference));
        connectToOtherBrokers();
        System.out.println("[INFO] Connected to " + nodesConnected.size() + " brokers.");
        System.out.println(nodesConnected);

        if(message.getNetworkState() == NetworkState.NETWORK_CONNECTED)
        {
            System.out.println("[INFO] Network has already started, I'm joining again");
            startRaft(true);
        }
    }

    /**
     * Handle a message of type {@code BrokersReadyMessage}.
     * @param message Message received.
     */
    private void onBrokersReadyMessage (BrokersReadyMessage message) {
        System.out.println("[INFO] Network ready to start: " + nodesConnected);

        startRaft(false); // The network is starting now
    }

    /**
     * Utility to start the execution of raft.
     *
     * @param networkAlreadyStarted True if the network has already started and this node is joining back after a crash.
     */
    private void startRaft(boolean networkAlreadyStarted)
    {
        System.out.println("Raft ready to start");

        // Start raft node
        raftNode = new RaftNode(brokerName, eventsQueue, this, networkAlreadyStarted);
        raftThread = new Thread() {
            public void run() {
                raftNode.waitForEvents();
            }
        };
        raftThread.start();
        System.out.println("[INFO] Raft thread started");
    }

    /**
     * Handle the disconnection of the code. If the disconnected node was the broker's leader, remove the reference.
     * @param disconnectedNode Name of the disconnected node.
     */
    public void handleDisconnection (String disconnectedNode) {
        if (this.leaderBroker != null && this.leaderBroker.equals(disconnectedNode)) {
            this.leaderBroker = null;
            System.out.println("[INFO] Leader broker disconnected.");

            eventsQueue.add(new Message(MessageType.LEADER_DISCONNECTED));
        }
    }

    /**
     * Setter for `leaderBroker`.
     * @param nodeId The id of the current leader of the network.
     */
    public void setLeaderBroker(String nodeId) {
        leaderBroker = nodeId;
    }

    /**
     * Function called from raft whenever the node becomes the new leader. Used
     * to notify the locator about the new leader's identity.
     */
    public void notifyLocatorImLeader()
    {
        final NodeReference nodeRef = new NodeReference(localIPAddress, brokerNetwork.getBrokerPublicPort(), brokerName, true);

        final NewElectedLeaderMessage msg = new NewElectedLeaderMessage(nodeRef);

        sendMessage("locator", msg);
    }

    /**
     * Add the reference of the {@code NodeReference} passed as parameter to the map associating each string (the name
     * of the node) to the corresponding {@code NodeReference} and print a message.
     * @param nodeReference Representation of the new connected node.
     */
    public void addNode (NodeReference nodeReference) {
        nodesConnected.put(nodeReference.nodeName(), nodeReference);
        System.out.println("[INFO] New node connected: " + nodeReference);
        System.out.println(nodesConnected);
    }

    /**
     * Connect to the other brokers already connected in the network, listed in the {@code nodesConnected} list.
     */
    private void connectToOtherBrokers() {
        nodesConnected.values().stream().filter(NodeReference::isBroker).forEach(nodeReference -> {
            brokerNetwork.connectToOtherBroker(nodeReference);
            try {
                sendMessage(nodeReference.nodeName(), new HelloRequestMessage(
                        Inet4Address.getLocalHost().getHostAddress(),
                        brokerNetwork.getBrokerPublicPort(),
                        brokerName,
                        true
                ));
            } catch (UnknownHostException e) {
                System.out.println("[EXCEPTION] " + e.getMessage());
            }
        });
    }

    /**
     * Used to recreate the queues from the raft log.
     * @param listOperations The list of operations used to recreate the queues.
     */
    public void recreateQueuesFromLog(final List<Operation> listOperations)
    {
        queueManager.recreateFromLog(listOperations);
    }

    /**
     * Handle a request received from a client. Sends immediately a
     * reply to the client in case the request is not valid. Otherwise, it
     * sends the request to the raft network to be processed.
     *
     * @param request The request from the client.
     */
    private void onClientRequest(final Message request)
    {
        String errorMessage = null;
        final String clientName;

        // First check if the operation is valid, set
        // infoMessage if an error is found
        switch (request.type)
        {
            case CREATE_QUEUE_REQUEST -> {
                final CreateQueueRequest r = (CreateQueueRequest) request;
                clientName = r.getClientName();
                try { queueManager.tryCreateQueue(r.getQueueName()); }
                catch (NameAlreadyUsedException e) { errorMessage = e.getMessage(); }
            }
            case APPEND_QUEUE_REQUEST -> {
                final AppendQueueRequest r = (AppendQueueRequest) request;
                clientName = r.getClientName();
                try { queueManager.tryAppendQueue(r.getQueueName()); }
                catch (QueueNotFoundException e) { errorMessage = e.getMessage(); }
            }
            case READ_QUEUE_REQUEST -> {
                final ReadQueueRequest r = (ReadQueueRequest) request;
                clientName = r.getClientName();
                try { queueManager.tryReadQueue(r.getQueueName(), r.getClientName()); }
                catch (QueueNotFoundException | EndOfQueueException e) { errorMessage = e.getMessage(); }
            }
            default -> throw new RuntimeException("Message type not supported by onClientRequest()");
        }

        // Check the outcome, if the operation is not valid send
        // negative response to the client
        if(errorMessage != null)
        {
            final Message response;
            switch (request.type)
            {
                case CREATE_QUEUE_REQUEST -> response = new CreateQueueResponse(false, errorMessage);
                case APPEND_QUEUE_REQUEST -> response = new AppendQueueResponse(false, errorMessage);
                case READ_QUEUE_REQUEST -> response = new ReadQueueResponse(0,false, errorMessage);
                default -> throw new RuntimeException("Message type not supported by onClientRequest()");
            }

            // Send response message back to the client
            System.out.println("[INFO] Invalid request from client: " + errorMessage);
            sendMessage(clientName, response);
            return;
        }

        // The request is valid, send it to raft to be processed
        eventsQueue.add(createRaftAppendMessage(request, clientName));
    }

    /**
     * Utility used to create a RaftAppendMessage from a VALID client request.
     *
     * @param request The request from the client. Must be a valid one, not checked here.
     * @param clientName The name of the client requesting the operation.
     * @return The RaftAppendMessage to be sent to raft.
     */
    private RaftAppendMessage createRaftAppendMessage(final Message request, final String clientName)
    {
        final RaftAppendMessage appendMessage;
        switch (request.type)
        {
            case CREATE_QUEUE_REQUEST -> {
                final CreateQueueRequest r = (CreateQueueRequest) request;
                appendMessage = new RaftAppendMessage(new CreateQueue(r.getQueueName(), clientName));
            }
            case APPEND_QUEUE_REQUEST -> {
                final AppendQueueRequest r = (AppendQueueRequest) request;
                // TODO: raft currently supports only APPEND WITH 1 VALUE, while the
                //  client commands interpreter and the AppendQueueRequest takes multiple
                //  values. Decide which way to go, for now only the first value is taken,
                //  following raft's convention.
                appendMessage = new RaftAppendMessage(new AppendQueue(r.getQueueName(), r.getAppendElements().get(0), clientName));
            }
            case READ_QUEUE_REQUEST -> {
                final ReadQueueRequest r = (ReadQueueRequest) request;
                appendMessage = new RaftAppendMessage(new ReadQueue(r.getClientName(), r.getQueueName(), clientName));
            }
            default -> throw new RuntimeException("Message type not supported by createRaftAppendMessage()");
        }

        return appendMessage;
    }

    /**
     * Used by raft to signal the BrokerController that an
     * operation has been committed.
     * A response message is sent to the client that requested
     * such operation.
     * THIS FUNCTION SHOULD BE INVOKED ONLY BY THE FOLLOWERS
     * OF THE NETWORK.
     *
     * @param operation The operation that was committed.
     */
    public void commitOperationFollower(final Operation operation)
    {
        if(Objects.equals(leaderBroker, brokerName))
        {
            throw new RuntimeException("This function should be called only by a follower");
        }

        switch (operation.getType())
        {
            case READ_QUEUE -> {
                final ReadQueue op = (ReadQueue) operation;
                try { queueManager.commitRead(op.queueName, op.readerName); }
                catch (Exception e) { e.printStackTrace(); } // It shouldn't fail, it should be tested before
            }
            case APPEND_QUEUE -> {
                final AppendQueue op = (AppendQueue) operation;
                try { queueManager.commitAppend(op.queueName, op.value); }
                catch (Exception e) { e.printStackTrace(); } // It shouldn't fail, it should be tested before
            }
            case CREATE_QUEUE -> {
                final CreateQueue op = (CreateQueue) operation;
                try { queueManager.commitCreate(op.queueName); }
                catch (Exception e) { e.printStackTrace(); } // It shouldn't fail, it should be tested before
            }
        }
    }

    /**
     * Used by raft to signal the BrokerController that an
     * operation has been committed.
     * A response message is sent to the client that requested
     * such operation.
     * THIS FUNCTION SHOULD BE INVOKED ONLY BY THE CURRENT
     * LEADER OF THE NETWORK.
     *
     * @param operation The operation that was committed.
     */
    public void commitOperationLeader(final Operation operation)
    {
        if(!Objects.equals(leaderBroker, brokerName))
        {
            throw new RuntimeException("This function should be invoked only by the leader of the network");
        }

        final String clientName = operation.getClientName();

        // Send response to the client requesting the operation

        Message response;
        switch (operation.getType())
        {
            case CREATE_QUEUE -> {
                CreateQueue op = (CreateQueue)operation;
                response = new CreateQueueResponse(true);

                try { queueManager.commitCreate(op.queueName); }
                catch (Exception e) { e.printStackTrace(); } // Cannot fail, already tested before
            }
            case APPEND_QUEUE -> {
                AppendQueue op = (AppendQueue) operation;
                response = new AppendQueueResponse(true);

                try { queueManager.commitAppend(op.queueName, op.value); }
                catch (Exception e) { e.printStackTrace(); } // Cannot fail, already tested before
            }
            case READ_QUEUE -> {
                ReadQueue op = (ReadQueue) operation;

                Integer value = 0;

                try { value = queueManager.commitRead(op.queueName, op.readerName); }
                catch (Exception e) { e.printStackTrace(); } // Cannot fail, already tested before

                response = new ReadQueueResponse(value, true);
            }
            default -> throw new RuntimeException("Operation type not supported"); // Used to suppress java warnings
        }

        System.out.println("[INFO] Sending positive response to the client: " + operation.toString());
        sendMessage(clientName, response);
    }

    /**
     * Get the list of brokers connected. Needed by Raft.
     *
     * @return The id of the nodes connected.
     */
    public Set<String> getBrokersConnected()
    {
        // TODO: access to nodesConnected should be synchronized?
        HashSet<String> retSet = new HashSet<>();

        for(String nodeId : nodesConnected.keySet())
        {
            if(nodesConnected.get(nodeId).isBroker())
            {
                retSet.add(nodeId);
            }
        }

        return retSet;
    }

    /**
     * @return The number of brokers (the caller is included)
     * connected at the moment.
     */
    public int getNumberOfBrokersConnected()
    {
        // TODO: access to nodesConnected should be synchronized?
        int ret = 0;

        for(String nodeId : nodesConnected.keySet())
        {
            if(nodesConnected.get(nodeId).isBroker())
            {
                ret++;
            }
        }

        return ret;
    }

    /**
     * Utility used to print the queues. Called by raft after
     * committing some messages.
     */
    public void printQueues()
    {
        queueManager.printQueues();
    }
}
