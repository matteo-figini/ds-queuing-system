package raft;
import messages.Message;
import messages.MessageType;
import messages.application.LogRequest;
import messages.application.LogResponse;
import messages.application.VoteRequest;
import messages.application.VoteResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class defines the logic of a Raft node.
 * @param <T> The type of the LogItem that will be stored in the node's log.
 */
public class RaftNode<T> {

    /** Number of nodes in the raft network */
    private static final Integer NUM_NODES = 5;

    /**
     * Value of the election timeout (how much time the election phase has before
     * aborting). Expressed in milliseconds.
     * TODO: is 5s fine?
     */
    private static final Integer ELECTION_TIMEOUT_VALUE = 5000;

    /**
     * Value of the leader heartbeat timeout (how much time has to pass before
     * followers suspect leader failure). Expressed in milliseconds.
     * TODO: is 3s fine?
     */
    private static final Integer LEADER_HEARTBEAT_TIMEOUT_VALUE = 3000;

    /**
     * Min value of the random offset to be added to LEADER_HEARTBEAT_TIMEOUT_VALUE.
     * Expressed in milliseconds.
     * TODO: is 250ms fine?
     */
    private static final Integer LEADER_HEARTBEAT_TIMEOUT_MIN_RAND = 250;

    /**
     * Max value of the random offset to be added to LEADER_HEARTBEAT_TIMEOUT_VALUE.
     * Expressed in milliseconds.
     * TODO: is 1s fine?
     */
    private static final Integer LEADER_HEARTBEAT_TIMEOUT_MAX_RAND = 1000;

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
     * Checks if the election process took too much time. In that case
     * the election is aborted.
     */
    TimeoutChecker<Message> electionTimeoutHandler;

    /**
     * Used by followers, checks if too much time has passed since the last
     * message received from the leader.
     */
    TimeoutChecker<Message> leaderHeartbeatTimeoutHandler;

    /**
     * Reference to the queue where events are published.
     */
    private LinkedBlockingQueue<Message> eventsQueue;

    /**
     * Class constructor.
     *
     * @param nodeId The univoqe id of the node being created.
     * @param nodesList The list of id of all the other nodes.
     * @param eventsQueue The reference to the queue where raft events are published.
     */
    public RaftNode(String nodeId, ArrayList<String> nodesList, LinkedBlockingQueue<Message> eventsQueue)
    {
        // TODO: are these assertions needed?
        if(NUM_NODES % 2 == 0 || NUM_NODES < 3)
        {
            throw new RuntimeException("NUM_NODES should be and odd number >1");
        }

        this.nodeId = nodeId;
        this.nodesList = nodesList;
        this.eventsQueue = eventsQueue;

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

        // Start leader heartbeat check
        startNewHeartbeatTimeout();
    }

    /**
     * This function waits on eventsQueue for new events to be handled.
     */
    public void waitForEvents()
    {
        while(true)
        {
            try
            {
                Message m = eventsQueue.take();

                switch (m.type)
                {
                    default:
                        break;
                    case VOTE_REQUEST:
                        this.onVoteRequest((VoteRequest) m);
                        break;
                    case VOTE_RESPONSE:
                        this.onVoteResponse((VoteResponse) m);
                        break;
                    case LOG_REQUEST:
                        this.onLogRequest((LogRequest<T>) m);
                        break;
                    case LOG_RESPONSE:
                        this.onLogResponse((LogResponse) m);
                        break;
                    case ELECTION_TIMEOUT:
                        this.onElectionTimeout();
                        break;
                    case LEADER_HEARTBEAT_TIMEOUT:
                        this.onLeaderTimeout();
                        break;
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Utility to handle the creation of the leader heartbeat timeout.
     */
    private void startNewHeartbeatTimeout()
    {
        leaderHeartbeatTimeoutHandler = new TimeoutChecker<>(eventsQueue,
                LEADER_HEARTBEAT_TIMEOUT_VALUE + ThreadLocalRandom.current().nextInt(
                        LEADER_HEARTBEAT_TIMEOUT_MIN_RAND, LEADER_HEARTBEAT_TIMEOUT_MAX_RAND + 1),
                new Message(MessageType.LEADER_HEARTBEAT_TIMEOUT),
                TimeoutChecker.Mode.SINGLE);
        leaderHeartbeatTimeoutHandler.start();
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

        // TODO: send message to all nodes
        if(true)
            throw  new UnsupportedOperationException();

        // Start election timer
        electionTimeoutHandler = new TimeoutChecker<>(eventsQueue, ELECTION_TIMEOUT_VALUE, new Message(MessageType.ELECTION_TIMEOUT), TimeoutChecker.Mode.SINGLE);
        electionTimeoutHandler.start();
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

    private void onNewLeader()
    {
        // It should be already stopped, but I want to be safe
        leaderHeartbeatTimeoutHandler.stop();

        // Start new leader heartbeat check
        startNewHeartbeatTimeout();
    }

    /**
     * This function handles the vote request received from another node.
     *
     * @param voteReq The vote request message object.
     */
    private void onVoteRequest(final VoteRequest voteReq)
    {
        // Signal message received
        // I don't stop the timer because the candidate might fail during the election.
        // That would leave the other followers waiting for him to become the leader.
        // Should I start an election timer on the followers instead of signaling the reception?
        leaderHeartbeatTimeoutHandler.eventReceived();

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

        diskBackupHandler.saveStatus(currentTerm, votedFor, commitLength);

        // TODO
//        reply(new VoteResponse(nodeId, currentTerm, vote), cId); // send reply to candidateId
        throw new UnsupportedOperationException();
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
                currentRole = NodeState.LEADER;
                currentLeader = nodeId;

                // Cancel election timer
                electionTimeoutHandler.stop();

                for(String followerId : nodesList)
                {
                    if(!Objects.equals(followerId, nodeId))
                    {
                        sentLength.put(followerId, log.size());
                        ackedLength.put(followerId, 0);

                        // TODO
//                        replicateLog();
                        throw new UnsupportedOperationException();
                    }
                }
            }
        }
        else if(term > currentTerm)
        {
            // Found a node with a higher term number
            currentTerm = term;
            currentRole = NodeState.FOLLOWER;
            votedFor = null;

            // Cancel election timer
            electionTimeoutHandler.stop();
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

        List<LogItem<T>> suffix = log.subList(prefixLen, log.size());

        int prefixTerm = 0;
        if(prefixLen > 0)
        {
            prefixTerm = log.get(prefixLen - 1).term;
        }

        // TODO
        // send() to followerId
        throw new UnsupportedOperationException();
    }

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
        if(logRequest.term > currentTerm)
        {
            currentTerm = logRequest.term;
            votedFor = null; // TODO: is it consistent?

            // Cancel election timer
            electionTimeoutHandler.stop();
        }

        if(logRequest.term.equals(currentTerm))
        {
            currentRole = NodeState.FOLLOWER;
            currentLeader = logRequest.leaderId;
        }

        // Reset leader heartbeat timer
        leaderHeartbeatTimeoutHandler.eventReceived();

        final Integer prefixLen = logRequest.prefixLen;
        final boolean logOk = (log.size() >= prefixLen) &&
                (prefixLen == 0 || Objects.equals(log.get(prefixLen - 1).term, logRequest.prefixTerm));
        if(logRequest.term.equals(currentTerm) && logOk)
        {
            appendEntries(prefixLen, logRequest.leaderCommit, logRequest.suffix);

            final Integer ack = logRequest.prefixLen + logRequest.suffix.size();
            // TODO: send operation successful to leader
            // send(new LogResponse(nodeId, currentTerm, ack, true));
            if(true)
                throw new UnsupportedOperationException();
        }
        else
        {
            // TODO: send operation failed to leader
            // send(new LogResponse(nodeId, currentTerm, 0, false));
            if(true)
                throw new UnsupportedOperationException();
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
                throw new UnsupportedOperationException();
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
            electionTimeoutHandler.stop();
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
                // TODO: deliver log[commitLength].msg to the application
                if(true)
                    throw new UnsupportedOperationException();
                commitLength++;
            }
            else
            {
                keepGoing = false;
            }
        }
    }
}
