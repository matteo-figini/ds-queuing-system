package raft;
import messages.application.LogRequest;
import messages.application.LogResponse;
import messages.application.VoteRequest;
import messages.application.VoteResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RaftNode<T> {

    /** Number of nodes in the raft network, decided at compile time */
    private final Integer NUM_NODES = 5;

    /** Used to distinguish node*/
    private Integer nodeId;

    /** List of nodeId of the other nodes in the network*/
    private ArrayList<Integer> nodesList;

    private NodeState currentRole = NodeState.FOLLOWER;

    private Integer currentTerm = 0;

    /** Id of the node we last have voted for*/
    private Integer votedFor = 0;

    /** Log of the node*/
    private ArrayList<LogItem<T>> log = new ArrayList<>();

    /** TODO */
    private Integer commitLength = 0;

    /** Id of the current leader node*/
    private Integer currentLeader = 0;

    /** Votes received by this node during the election*/
    private ArrayList<Integer> votesReceived = new ArrayList<>();

    /**
     * Number of log records that we have already sent to a particular node.
     * The key is the nodeId of the receiving node, the value is the actual length.
     */
    private HashMap<Integer, Integer> sentLength = new HashMap<>();

    /**
     * Number of log entries that a particular node acknowledged as having received.
     * The key is the nodeId of the node sending acks, the value is the actual number.
     */
    private HashMap<Integer, Integer> ackedLength = new HashMap<>();

    public RaftNode(Integer nodeId, ArrayList<Integer> nodesList) {

        // TODO: are these assertions needed?
        if(nodeId <= 0)
        {
            throw new RuntimeException("nodeId should be >0");
        }
        if(NUM_NODES % 2 == 0 || NUM_NODES < 3)
        {
            throw new RuntimeException("NUM_NODES should be and odd number >1");
        }

        this.nodeId = nodeId;
        this.nodesList = nodesList;

        if (logFileDetected()) {
            recoverFromCrash();
        } else
        {
            init();
        }
    }

    /**
     * This function is used to initialize a newly created node of the network.
     */
    private void init()
    {
        currentRole = NodeState.FOLLOWER;
        currentTerm = 0;
        votedFor = 0;
        log.clear();
        commitLength = 0;
        currentLeader = 0;
        votesReceived.clear();
        sentLength.clear();
        ackedLength.clear();
    }

    /**
     * This function is used to check if there is already a log file stored by a previous
     * instance of this node (sharing the same nodeId).
     *
     * @return True if log file is found, return false otherwise.
     */
    private boolean logFileDetected()
    {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    /**
     * This function is used to recover the state of a crashed node from the log file
     * on disk.
     */
    private void recoverFromCrash()
    {
        currentRole = NodeState.FOLLOWER;
        currentLeader = 0;
        votesReceived.clear();
        sentLength.clear();
        ackedLength.clear();

        // Recover other state variables from log file

        // TODO: continue implementing
        throw new UnsupportedOperationException();
    }

    /**
     * This function handles the case of no message received from the leader after
     * an extended period of time. The node goes into candidate state and starts
     * an election.
     */
    public void onLeaderTimeout()
    {
        currentTerm += 1;
        currentRole = NodeState.CANDIDATE;
        votedFor = nodeId;

        votesReceived.clear(); // TODO: I added this, is it actually needed?
        votesReceived.add(nodeId);

        Integer lastTerm = 0;
        if(log.size() > 0)
        {
            lastTerm = log.get(log.size() - 1).term;
        }

        // TODO
        // send message to all nodes
        // start election timer
        throw  new UnsupportedOperationException();
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
        if(log.size() > 0)
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
        final Integer voterId = voteReply.voterId;
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

                // TODO: cancel election timer
                if(true)
                {
                    // The "if" is needed to ignore the "unreachable statement" error while
                    // waiting to implement the timer election functionality
                    throw new UnsupportedOperationException();
                }

                for(Integer followerId : nodesList)
                {
                    if(followerId != nodeId)
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
            // TODO: cancel election timer
            throw new UnsupportedOperationException();
        }
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

        for(Integer followerId : nodesList)
        {
            if(!Objects.equals(followerId, nodeId)) // Avoid leader sending to himself
            {
                replicateLog(followerId);
            }
        }
    }

    /**
     * Called on the leader whenever there is a new message in the log, and also
     * periodically. If there are no new messages, suffix is the empty list, serving
     * as heartbeats.
     *
     * @param followerId The id of the follower that will receive the message.
     */
    private void replicateLog(Integer followerId)
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
    public void onLogRequest(final LogRequest<T> logRequest)
    {
        if(logRequest.term > currentTerm)
        {
            currentTerm = logRequest.term;
            votedFor = null; // TODO: is it consistent?

            // TODO: cancel election timer
            throw new UnsupportedOperationException();
        }

        if(logRequest.term == currentTerm)
        {
            currentRole = NodeState.FOLLOWER;
            currentLeader = logRequest.leaderId;
        }

        final Integer prefixLen = logRequest.prefixLen;
        final boolean logOk = (log.size() >= prefixLen) &&
                (prefixLen == 0 || log.get(prefixLen - 1).term == logRequest.prefixTerm);
        if(logRequest.term == currentTerm && logOk)
        {
            appendEntries(prefixLen, logRequest.leaderCommit, logRequest.suffix);

            final Integer ack = logRequest.prefixLen + logRequest.suffix.size();
            // TODO: send operation successful to leader
            // send(new LogResponse(nodeId, currentTerm, ack, true));
            throw new UnsupportedOperationException();
        }
        else
        {
            // TODO: send operation failed to leader
            // send(new LogResponse(nodeId, currentTerm, 0, false));
            throw new UnsupportedOperationException();
        }
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

        if(suffix.size() > 0 && log.size() > prefixLen)
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
    }

    /**
     * This function is used to handle log response messages sent by the followers to the
     * leader.
     *
     * @param logResponse The response message.
     */
    public void onLogResponse(final LogResponse logResponse)
    {
        if(logResponse.term == currentTerm && currentRole == NodeState.LEADER)
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
            // TODO: cancel election timer
            throw  new UnsupportedOperationException();
        }
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
            for(Integer node : nodesList)
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
