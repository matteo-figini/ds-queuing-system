package raft;
import broker.BrokerController;
import broker.BrokerNetwork;
import messages.Message;
import messages.MessageType;
import messages.application.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class defines the logic of a Raft node.
 * @param <T> The type of the LogItem that will be stored in the node's log.
 */
public class RaftNode<T> {

    /** Number of nodes in the raft network */
    // TODO: set from BrokerMain
    private static final Integer NUM_NODES = 3;

    /**
     * Value of the election timeout for the candidate (how much time the election
     * phase has before aborting). Expressed in milliseconds.
     * TODO: is 5s fine?
     */
    private static final Integer ELECTION_OUT_OF_TIME_TIMEOUT_CANDIDATE = 5000;

    /**
     * Value of the election timeout for the followers: how much time the election can take
     * before the followers start suspecting a candidate's failure. Expressed in milliseconds.
     * Gives an extra second with respect to the candidate value to take into account
     * connection slowness.
     * TODO: is it fine?
     */
    private final Integer ELECTION_OUT_OF_TIME_TIMEOUT_FOLLOWER;

    private final Integer ASK_LEADER_REQUEST_TIMEOUT;

    /**
     * Min value of the range used to generate random timeouts. Expressed in milliseconds.
     */
    private static final Integer TIMEOUT_MIN_RAND = 300;

    /**
     * Max value of the range used to generate random timeouts. Expressed in milliseconds.
     */
    private static final Integer TIMEOUT_MAX_RAND = 1800;

    /** Used to distinguish node*/
    private final String nodeId;

    /** List of nodeId of the other nodes in the network*/
    private final ArrayList<String> nodesList;

    private NodeState currentRole;

    private Integer currentTerm = 0;

    /** Id of the node we last have voted for*/
    private String votedFor = null;

    /** Log of the node*/
    private ArrayList<LogItem<T>> log = new ArrayList<>();

    /** TODO */
    private Integer commitLength = 0;

    /** Id of the current leader node*/
    private String currentLeader = null;

    /** Votes received by this node during the election*/
    private ArrayList<String> votesReceived = new ArrayList<>();

    /**
     * Number of log records that we have already sent to a particular node.
     * The key is the nodeId of the receiving node, the value is the actual length.
     */
    private HashMap<String, Integer> sentLength = new HashMap<>();

    /**
     * Number of log entries that a particular node acknowledged as having received.
     * The key is the nodeId of the node sending acks, the value is the actual number.
     */
    private HashMap<String, Integer> ackedLength = new HashMap<>();

    /**
     * Handles the backup operations on disk.
     */
    final private LogFilesHandler<LogItem<T>> diskBackupHandler;

    /**
     * Reference to the queue where events are published.
     */
    private LinkedBlockingQueue<Message> eventsQueue;

    /**
     * Reference to the brokerController, used to communicate with other nodes.
     */
    private final BrokerController brokerController;

    /* ---------- TIMEOUT CHECKERS ---------- */

    /**
     * Checks if the election process took too much time. In that case
     * the election is aborted.
     */
    private TimeoutChecker timeoutHandlerElectionCandidate;

    /**
     * Checks if the election process of another node (the candidate node)
     * takes too much time, in which case it might have failed. In that case
     * the node tries to start another election.
     */
    private TimeoutChecker timeoutHandlerElectionFollower;

    /**
     * This timeout is started when the leader disconnection event is received.
     * In that case we start a random timeout, the first node to trigger that
     * timeout starts an election. The others should receive the vote request,
     * stop their timeout checkers and vote for the candidate.
     */
    private TimeoutChecker timeoutHandlerLeaderDisconnected;

    /**
     * This timeout is started when a node is joining an already running
     * network. It sends a AskLeaderRequest message to all active brokers,
     * then it waits for a response.
     */
    private TimeoutChecker timeoutHandlerAskLeaderRequest;

    /**
     * Class constructor.
     *
     * @param nodeId The univoqe id of the node being created.
     * @param nodesList The list of id of all the other nodes.
     * @param eventsQueue The reference to the queue where raft events are published.
     * @param brokerController
     * @param netAlreadyStarted True if the network has already started and this node is joining back after a crash.
     */
    public RaftNode(String nodeId, ArrayList<String> nodesList, LinkedBlockingQueue<Message> eventsQueue, BrokerController brokerController, boolean netAlreadyStarted)
    {
        // TODO: are these assertions needed?
        if(NUM_NODES % 2 == 0 || NUM_NODES < 3)
        {
            throw new RuntimeException("NUM_NODES should be and odd number >1");
        }

        this.nodeId = nodeId;
        this.nodesList = nodesList;
        this.eventsQueue = eventsQueue;
        this.brokerController = brokerController;

        Random rand = new Random();
        // Obtain a number between [0 - 49].
        final int randTimeout = rand.nextInt(TIMEOUT_MIN_RAND, TIMEOUT_MAX_RAND);
        ELECTION_OUT_OF_TIME_TIMEOUT_FOLLOWER = ELECTION_OUT_OF_TIME_TIMEOUT_CANDIDATE + randTimeout + 500;
        ASK_LEADER_REQUEST_TIMEOUT = ELECTION_OUT_OF_TIME_TIMEOUT_FOLLOWER;

        timeoutHandlerLeaderDisconnected = new TimeoutChecker(eventsQueue, randTimeout + 500, new Message(MessageType.START_ELECTION), TimeoutChecker.Mode.EXPLICIT);
        timeoutHandlerElectionFollower = new TimeoutChecker(eventsQueue, ELECTION_OUT_OF_TIME_TIMEOUT_FOLLOWER + randTimeout,
                new Message(MessageType.ELECTION_OUT_OF_TIME_FOLLOWER), TimeoutChecker.Mode.EXPLICIT);
        timeoutHandlerAskLeaderRequest = new TimeoutChecker(eventsQueue, ASK_LEADER_REQUEST_TIMEOUT, new Message(MessageType.LEADER_DISCONNECTED), TimeoutChecker.Mode.EXPLICIT);

        // Init backup
        diskBackupHandler = new LogFilesHandler<>(nodeId + "log.dat", nodeId + "status.dat");

        // Init node
        currentRole = NodeState.FOLLOWER;
        currentTerm = 0;
        votedFor = null;
        commitLength = 0;
        currentLeader = null;

        if (diskBackupHandler.logExists()) {
            recoverFromCrash();
        } else {
            // Save initial state to disk
            diskBackupHandler.saveStatus(currentTerm, votedFor, commitLength);
            diskBackupHandler.saveLog(log);
        }

        if(!netAlreadyStarted)
        {
            // If the network isn't already running there is no leader,
            // so I manually trigger an election
            onLeaderDisconnection();
        }
        else
        {
            // The network has already started, ask others
            // who the current leader is
            askLeader();
        }
    }

    /**
     * This function waits on eventsQueue for new events to be handled.
     */
    public void waitForEvents()
    {
        System.out.println("[INFO] Raft node started, waiting for events");
        while(true)
        {
            Message m = null;
            boolean messageReceived = true;

            try
            {
                m = eventsQueue.take();
            }
            catch (InterruptedException e)
            {
                messageReceived = false;
                //Thread.sleep(10);
            }

            if(messageReceived)
            {
                switch (m.type)
                {
                    default:
                        break;
                    case VOTE_REQUEST:
                        System.out.println("[INFO] Received vote request");
                        this.onVoteRequest((VoteRequest) m);
                        break;
                    case START_ELECTION:
                        System.out.println("[INFO] Starting an election");
                        this.onLeaderTimeout();
                        break;
                    case VOTE_RESPONSE:
                        System.out.println("[INFO] Received vote response");
                        this.onVoteResponse((VoteResponse) m);
                        break;
                    case LOG_REQUEST:
                        System.out.println("[INFO] Received log request");
                        this.onLogRequest((LogRequest<T>) m);
                        break;
                    case LOG_RESPONSE:
                        System.out.println("[INFO] Received log response");
                        this.onLogResponse((LogResponse) m);
                        break;
                    case ELECTION_OUT_OF_TIME_CANDIDATE:
                        if(!timeoutHandlerElectionCandidate.isDisabled())
                        {
                            // This timeout might become invalid because this
                            // node has received a vote response from a node
                            // with a higher term -> this node cannot be a candidate
                            System.out.println("[INFO] Received election timeout event");
                            this.onElectionTimeout();
                        }
                        // If the timeout was disabled the event is ignored
                        // TODO: is this check needed? isn't it already checked by disableAndRemove()?
                        break;
                    case ELECTION_OUT_OF_TIME_FOLLOWER:
                        System.out.println("[INFO] Candidate's election took too much time, starting an election myself");
                        this.onLeaderDisconnection();
                        break;
//                    case LEADER_HEARTBEAT_TIMEOUT:
//                        System.out.println("[INFO] Received heartbeat timeout event");
//                        this.onLeaderTimeout();
//                        break;
                    case LEADER_DISCONNECTED:
                        System.out.println("[INFO] The leader has disconnected");
                        this.onLeaderDisconnection();
                        break;
                    case ASK_LEADER_REQUEST:
                        System.out.println("[INFO] Received ask leader request");
                        this.onAskLeaderRequest((AskLeaderRequest) m);
                        break;
                    case ASK_LEADER_RESPONSE:
                        System.out.println("[INFO] Received ask leader response");
                        this.onAskLeaderResponse((AskLeaderResponse) m);
                        break;
                }
            }
            else
            {
                // TODO: needed?
                try { Thread.sleep(5); }
                catch (Exception e) {}
            }

            System.out.println("[INFO] Current state: " + currentRole.name());
        }
    }

    /**
     * This function is used to recover the state of a crashed node from the log file
     * on disk.
     */
    private void recoverFromCrash()
    {
        currentRole = NodeState.FOLLOWER;
        currentLeader = null;
        votesReceived.clear();
        sentLength.clear();
        ackedLength.clear();

        // Recover other state variables from log file
        currentTerm = 0;
        votedFor = null;
        commitLength = 0;
        log.clear();
        try
        {
            diskBackupHandler.loadLog(log);

            final LogFilesHandler<LogItem<T>>.StatusStructure s = diskBackupHandler.loadStatus();

            currentTerm = s.currentTerm;
            votedFor = s.votedFor;
            commitLength = s.commitLength;
        }
        catch (IOException e)
        {
            // TODO: improve?
            System.out.println(e.getMessage());
        }
    }

    private void askLeader()
    {
        brokerController.sendMessage(BrokerNetwork.ALL_BROKERS_CMD, (Message) new AskLeaderRequest(nodeId));

        // Start timeout
        timeoutHandlerAskLeaderRequest.startNewTimeout();
    }

    private void onAskLeaderRequest(final AskLeaderRequest msg)
    {
        final AskLeaderResponse response = new AskLeaderResponse(currentLeader);

        brokerController.sendMessage(msg.sender, response);
    }

    private void onAskLeaderResponse(final AskLeaderResponse msg)
    {
        if(currentLeader != null)
        {
            // This node has already received the info from
            // another node, there's no need to process another message
            return;
        }

        // Stop the timeout for the request
        timeoutHandlerAskLeaderRequest.disableAndRemove();

        currentLeader = msg.leaderName;
        brokerController.setLeaderBroker(currentLeader);
        System.out.println("[INFO] Received ask leader response, leader: " + currentLeader);

        if(currentLeader == null)
        {
            // There is no actual leader
            onLeaderDisconnection();
            // TODO: se ricevo questo messaggio dopo che un elezione è in corso / è già avvenuta???
        }
    }

    /**
     * Handles the disconnection of the leader. A random timer is started, after which
     * the node might become a candidate or might receive a vote request from a node
     * that had a shorter random timer.
     */
    private void onLeaderDisconnection()
    {
        // The leader has disconnected. I start a timeout (which is random)
        // that might trigger an election if this node doesn't receive
        // a vote request before the timeout fires.
        timeoutHandlerLeaderDisconnected.startNewTimeout();

        currentLeader = null;
    }

    /**
     * This function handles the case of no message received from the leader after
     * an extended period of time. The node goes into candidate state and starts
     * an election.
     */
    private void onLeaderTimeout()
    {
        currentTerm += 1;
        currentRole = NodeState.CANDIDATE;
        votedFor = nodeId;

        votesReceived.clear(); // TODO: I added this, is it actually needed?
        votesReceived.add(nodeId);

        Integer lastTerm = 0;
        if(!log.isEmpty())
        {
            lastTerm = log.get(log.size() - 1).term;
        }

        diskBackupHandler.saveStatus(currentTerm, votedFor, commitLength);

        // send message to all nodes
        final VoteRequest voteMsg;
        if(!log.isEmpty())
            voteMsg = new VoteRequest(nodeId, currentTerm, log.size(), log.get(log.size() - 1).term);
        else
            voteMsg = new VoteRequest(nodeId, currentTerm, 0, 0); // TODO: lastTerm = 0 ?
        brokerController.sendMessage(BrokerNetwork.ALL_BROKERS_CMD, voteMsg);

        // Start election timer
        if(timeoutHandlerElectionCandidate != null && timeoutHandlerElectionCandidate.isRunning())
        {
            timeoutHandlerElectionCandidate.disableAndRemove();
            throw new RuntimeException("CAZZO electionTimeoutHandlerCandidate STAVA ANDANDO");
            // TODO: fix
        }
        timeoutHandlerElectionCandidate = new TimeoutChecker(eventsQueue,
                ELECTION_OUT_OF_TIME_TIMEOUT_CANDIDATE,
                new Message(MessageType.ELECTION_OUT_OF_TIME_CANDIDATE),
                TimeoutChecker.Mode.EXPLICIT);
        timeoutHandlerElectionCandidate.startNewTimeout();
    }

    /**
     * Called after receiving an election timeout event: the election took too much time,
     * abort and start a new one.
     */
    private void onElectionTimeout()
    {
        // Start a new election with a higher term
        onLeaderTimeout();
    }

    /**
     * This function handles the vote request received from another node.
     *
     * @param voteReq The vote request message object.
     */
    private void onVoteRequest(final VoteRequest voteReq)
    {
        final Integer cLogLastTerm = voteReq.cLogLastTerm;
        final Integer cLogLength = voteReq.cLogLength;

        if(voteReq.cTerm > currentTerm)
        {
            currentTerm = voteReq.cTerm;
            currentRole = NodeState.FOLLOWER;
            votedFor = null; // TODO: is it consistent?
        }

        Integer lastTerm = 0;
        if(!log.isEmpty())
        {
            lastTerm = log.get(log.size() - 1).term;
        }

        boolean logOk = (cLogLastTerm > lastTerm) ||
                (Objects.equals(cLogLastTerm, lastTerm) && cLogLength >= log.size());

        // True if this node voted for this candidate or none
        boolean votedForOk = votedFor == null || votedFor.equals(voteReq.cId); // TODO: is `null` consistent?

        boolean vote;
        if(voteReq.cTerm.equals(currentTerm) && logOk && votedForOk)
        {
            votedFor = voteReq.cId;
            vote = true;
        }
        else
        {
            vote = false;
        }

        if(vote)
        {
            // The vote is positive

            // Another node has started an election, and it can
            // be a leader, this node doesn't have to start
            // another election
            timeoutHandlerLeaderDisconnected.disableAndRemove();

            // Start an election timeout, which checks if the election takes too
            // much time (the candidate might have failed)
            if(timeoutHandlerElectionFollower.isRunning())
            {
                // If it was already started, stop it
                timeoutHandlerElectionFollower.disableAndRemove();
            }
            timeoutHandlerElectionFollower.startNewTimeout();
        }

        diskBackupHandler.saveStatus(currentTerm, votedFor, commitLength);

        final Message msg = new VoteResponse(nodeId, currentTerm, vote);
        brokerController.sendMessage(voteReq.cId, msg);
    }

    /**
     * This function handles the vote response message received from another node.
     *
     * @param voteReply The vote response message object.
     */
    private void onVoteResponse(final VoteResponse voteReply)
    {
        final String voterId = voteReply.voterId;
        final Integer term = voteReply.voterCurrentTerm;
        final boolean vote = voteReply.vote;

        if(currentRole == NodeState.CANDIDATE && Objects.equals(term, currentTerm) && vote)
        {
            votesReceived.add(voterId);

            if(votesReceived.size() >= (NUM_NODES + 1) / 2)
            {
                // Election won
                // TODO: remove print
                System.out.println("[INFO] ELECTION WON");

                currentRole = NodeState.LEADER;
                currentLeader = nodeId;
                brokerController.setLeaderBroker(currentLeader);

                // Cancel election timer
                timeoutHandlerElectionCandidate.disableAndRemove();

                // Send first message to each follower
                for(String followerId : nodesList)
                {
                    if(!Objects.equals(followerId, nodeId))
                    {
                        sentLength.put(followerId, log.size());
                        ackedLength.put(followerId, 0);

                        replicateLog(followerId);
                    }
                }

                brokerController.notifyLocatorImLeader();
            }
        }
        else if(term > currentTerm)
        {
            // Found a node with a higher term number
            currentTerm = term;
            currentRole = NodeState.FOLLOWER;
            votedFor = null;

            // Cancel election timer
            timeoutHandlerElectionCandidate.disableAndRemove();
        }

        diskBackupHandler.saveStatus(currentTerm, votedFor, commitLength);
    }

    /**
     * This function is used to append data to the log and propagate it to the followers.
     *
     * ATTENTION: this function doesn't cover the case where the followers can forward
     * append requests to the leader. We assume that clients communicate only with the leader,
     * thus only the leader should be able to handle such requests.
     *
     * @param newItem The Item to be added to the log.
     */
    public void onAppendMessage(final LogItem<T> newItem)
    {
        // TODO: remove this check
        if(currentRole != NodeState.LEADER)
        {
            throw new RuntimeException("Error, this function should be called only on the leader node");
        }

        log.add(newItem);

        // Update ackedLength of the leader
        ackedLength.put(nodeId, log.size());

        for(String followerId : nodesList)
        {
            if(!Objects.equals(followerId, nodeId)) // Avoid leader sending to himself
            {
                replicateLog(followerId);
            }
        }

        diskBackupHandler.saveLog(log);
    }

    /**
     * Called on the leader whenever there is a new message in the log, and also
     * periodically. If there are no new messages, suffix is the empty list, serving
     * as heartbeats.
     *
     * @param followerId The id of the follower that will receive the message.
     */
    private void replicateLog(String followerId)
    {
        // TODO: remove this check
        if(currentRole != NodeState.LEADER)
        {
            throw new RuntimeException("Error, this function should be called only on the leader node");
        }

        int prefixLen = sentLength.get(followerId);

        List<LogItem<T>> suffix = new ArrayList<>(log.subList(prefixLen, log.size()));

        int prefixTerm = 0;
        if(prefixLen > 0)
        {
            prefixTerm = log.get(prefixLen - 1).term;
        }

        // send to followerId
        // TODO: `commitLength` is it right?
        Message msg = new LogRequest<T>(currentLeader, currentTerm, prefixLen, prefixTerm, commitLength, suffix);
        brokerController.sendMessage(followerId, msg);
    }

    /**
     * Called on the leader whenever the heartbeat notification is received.
     */
//    private void sendHeartbeat()
//    {
//        // TODO: used?
//
//        // Assert
//        if(currentRole != NodeState.LEADER)
//        {
//            throw new RuntimeException("ERROR, sendHeartbeat() SHOULD BE CALLED ONLY ON THE LEADER");
//        }
//
//        for(String followerId : nodesList)
//        {
//            if(!Objects.equals(followerId, nodeId)) // Don't send to myself
//            {
//                replicateLog(followerId);
//            }
//        }
//    }

    /**
     * This function is used to handle log messages received from the leader. The followers
     * check if their logs are consistent with the leader. If so they append the new data
     * to their logs, otherwise they signal it to the leader in order to retrieve the missing
     * data.
     *
     * @param logRequest The message received from the leader.
     */
    private void onLogRequest(final LogRequest<T> logRequest)
    {
        // Check if there were timeouts running
        if(timeoutHandlerElectionFollower != null && timeoutHandlerElectionFollower.isRunning())
        {
            timeoutHandlerElectionFollower.disableAndRemove();
        }
        if(timeoutHandlerElectionCandidate != null && timeoutHandlerElectionCandidate.isRunning())
        {
            timeoutHandlerElectionCandidate.disableAndRemove();
        }
        if(timeoutHandlerLeaderDisconnected != null && timeoutHandlerLeaderDisconnected.isRunning())
        {
            timeoutHandlerLeaderDisconnected.disableAndRemove();
        }

        if(logRequest.term > currentTerm)
        {
            currentTerm = logRequest.term;
            votedFor = null; // TODO: is it consistent?

            // Cancel election timer
            // TODO: teoricamente posso rimuoverlo perchè non c'è più bisogno di ricevere l'heartbeat,
            //  sappiamo se il leader è morto tramite le socket
//            electionTimeoutHandler.stop();
        }

        if(logRequest.term.equals(currentTerm))
        {
            currentRole = NodeState.FOLLOWER;
            currentLeader = logRequest.leaderId;
            brokerController.setLeaderBroker(currentLeader);
        }

        final Integer prefixLen = logRequest.prefixLen;
        final boolean logOk = (log.size() >= prefixLen) &&
                (prefixLen == 0 || Objects.equals(log.get(prefixLen - 1).term, logRequest.prefixTerm));
        if(logRequest.term.equals(currentTerm) && logOk)
        {
            appendEntries(prefixLen, logRequest.leaderCommit, logRequest.suffix);

            final Integer ack = logRequest.prefixLen + logRequest.suffix.size();

            Message msg = new LogResponse(nodeId, currentTerm, ack, true);
            brokerController.sendMessage(logRequest.leaderId, msg);
        }
        else
        {
            Message msg = new LogResponse(nodeId, currentTerm, 0, false);
            brokerController.sendMessage(logRequest.leaderId, msg);
        }

        diskBackupHandler.saveStatus(currentTerm, votedFor, commitLength);
    }

    /**
     * Utility function that handles the insertion of new entries in
     * the follower's log.
     *
     * @param prefixLen Number of entries already inserted in the follower's log.
     * @param leaderCommit The number of entries committed by the leader.
     * @param suffix The list containing the entries to be added.
     */
    private void appendEntries(Integer prefixLen, Integer leaderCommit, List<LogItem<T>> suffix)
    {
        // TODO: this function should be called only by followers?
        //  Should I add an assertion for testing?

        if(!suffix.isEmpty() && log.size() > prefixLen)
        {
            int index = Math.min(log.size(), prefixLen + suffix.size()) - 1;
            if(log.get(index).term != suffix.get(index - prefixLen).term)
            {
                // log is inconsistent, keep only until prefixLen
                // TODO: is it correct? the slides say to keep until `prefixLen - 1` included?
                log = new ArrayList<>(log.subList(0, prefixLen));
            }
        }

        if(prefixLen + suffix.size() > log.size())
        {
            for(int i = log.size() - prefixLen; i < suffix.size(); i++)
            {
                log.add(suffix.get(i));
            }
        }

        if(leaderCommit > commitLength)
        {
            // TODO: is this part (the whole commit part) needed? Or we can assume that
            //  messages are delivered instantaneously to the application?

            for(int i = commitLength; i < leaderCommit; i++)
            {
                // deliver log[i].msg to the application
                // TODO: is this enough? It should be for now, at least for testing
                //  Also check if this is equivalent with what is done inside commitLogEntries()
                System.out.println("[INFO] New log entry committed from appendEntries(): " + log.get(i).msg);
            }

            commitLength = leaderCommit;
        }

        diskBackupHandler.saveLog(log);
    }

    /**
     * This function is used to handle log response messages sent by the followers to the
     * leader.
     *
     * @param logResponse The response message.
     */
    private void onLogResponse(final LogResponse logResponse)
    {
        if(Objects.equals(logResponse.term, currentTerm) && currentRole == NodeState.LEADER)
        {
            if(logResponse.outcome && logResponse.ack >= ackedLength.get(logResponse.nodeId))
            {
                sentLength.put(logResponse.nodeId, logResponse.ack);
                ackedLength.put(logResponse.nodeId, logResponse.ack);

                commitLogEntries(); // TODO: is this part (the whole commit part) needed?
            }
            else if(sentLength.get(logResponse.nodeId) > 0)
            {
                sentLength.put(logResponse.nodeId, sentLength.get(logResponse.nodeId) - 1);
                replicateLog(logResponse.nodeId);
            }
        }
        else if(logResponse.term > currentTerm)
        {
            currentTerm = logResponse.term;
            currentRole = NodeState.FOLLOWER;
            votedFor = null; // TODO: is it consistent?

            // Cancel election timer
            // TODO: check, why should i stop election timer in the leader?
//            electionTimeoutHandler.stop();
        }

        diskBackupHandler.saveStatus(currentTerm, votedFor, commitLength);
    }

    /**
     * Utility function that checks if any of the new log entries have been acknowledged
     * by a quorum of nodes. When the log entry is committed its message is delivered to
     * the application.
     */
    private void commitLogEntries()
    {
        // Note: the code of this function was taken from the video, not from the pdf

        boolean keepGoing = true;

        while(commitLength < log.size() && keepGoing)
        {
            int acks = 0;
            for(String node : nodesList)
            {
                if(ackedLength.get(node) > commitLength)
                {
                    acks++;
                }
            }

            if(acks > (nodesList.size() + 1) / 2)
            {
                // deliver log[commitLength].msg to the application
                // TODO: is this enough? It should be for now, at least for testing.
                //  Also check if this is equivalent with what is done inside appendEntries()
                System.out.println("[INFO] New log entry committed from commitLogEntries(): " + log.get(commitLength).msg);
                commitLength++;
            }
            else
            {
                keepGoing = false;
            }
        }
    }
}
