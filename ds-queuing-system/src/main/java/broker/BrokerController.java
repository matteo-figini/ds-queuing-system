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
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;

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
    private RaftNode<Operation> raftNode;
    Thread raftThread;
    private LinkedBlockingQueue<Message> eventsQueue = new LinkedBlockingQueue<>();

    // Application stuff
    AppQueueManager queueManager = new AppQueueManager();

    /**
     * This hashmap is used to link the operation that has to be
     * executed. This operation is evaluated by the raft network and
     * is considered completed when the raft network commits it.
     * Once committed, the operation id can lead to the client
     * that requested the operation.
     */
    private final HashMap<Integer, String> mapOperationsClients = new HashMap<>();

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
        raftNode = new RaftNode<>(brokerName, nodesConnected.keySet(), eventsQueue, this, networkAlreadyStarted);
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
        // TODO: unused at the moment
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

        // Get an id for the operation
        final Integer operationId = raftNode.getValidOperationId();

        // Store the client name in the map
        mapOperationsClients.put(operationId, clientName);

        eventsQueue.add(createRaftAppendMessage(request, operationId));
    }

    /**
     * Utility used to create a RaftAppendMessage from a VALID client request.
     *
     * @param request The request from the client. Must be a valid one, not checked here.
     * @param operationId The id for the operation to be created.
     * @return The RaftAppendMessage to be sent to raft.
     */
    private RaftAppendMessage createRaftAppendMessage(final Message request, final Integer operationId)
    {
        final RaftAppendMessage appendMessage;
        switch (request.type)
        {
            case CREATE_QUEUE_REQUEST -> {
                final CreateQueueRequest r = (CreateQueueRequest) request;
                appendMessage = new RaftAppendMessage(new CreateQueue(r.getQueueName(), operationId));
            }
            case APPEND_QUEUE_REQUEST -> {
                final AppendQueueRequest r = (AppendQueueRequest) request;
                // TODO: raft currently supports only APPEND WITH 1 VALUE, while the
                //  client commands interpreter and the AppendQueueRequest takes multiple
                //  values. Decide which way to go, for now only the first value is taken,
                //  following raft's convention.
                appendMessage = new RaftAppendMessage(new AppendQueue(r.getQueueName(), r.getAppendElements().get(0), operationId));
            }
            case READ_QUEUE_REQUEST -> {
                final ReadQueueRequest r = (ReadQueueRequest) request;
                appendMessage = new RaftAppendMessage(new ReadQueue(r.getClientName(), r.getQueueName(), operationId));
            }
            default -> throw new RuntimeException("Message type not supported by createRaftAppendMessage()");
        }

        return appendMessage;
    }

    /**
     * Used by raft to signal the BrokerController that a create
     * queue operation has been committed.
     * A response message is sent to the client that requested
     * such operation.
     * @param operation The operation that was committed.
     */
    public void commitOperation(final Operation operation)
    {
        final String clientName = mapOperationsClients.get(operation.getId());

        // Remove from the pending operations
        mapOperationsClients.remove(operation.getId());

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

        // TODO
        System.out.println("[INFO] Sending positive response to the client: " + operation.toString());
        sendMessage(clientName, response);
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
