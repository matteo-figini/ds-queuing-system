package raft;

import messages.Message;
import messages.MessageType;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is a wrapper around the TimeoutCheckers needed
 * for Raft to work.
 * It handles the creation, start and stop of all the needed
 * timeouts.
 */
public class RaftTimeoutManager {

    /**
     * The actual timeout checker.
     */
    private TimeoutChecker timeoutChecker;

    /**
     * After detecting a disconnection from the leader all the nodes start a
     * timeout. The first one that fires, starts an election.
     */
    private final int leaderDisconnectedTimeout;

    /**
     * Value of the election timeout for the followers: how much time the election can take
     * before the followers start suspecting a candidate's failure. Expressed in milliseconds.
     * Gives an extra second with respect to the candidate value to take into account
     * connection slowness.
     */
    private final int electionFollowerTimeout;

    /**
     * Value of the election timeout for the candidate (how much time the election
     * phase has before aborting). Expressed in milliseconds.
     */
    private final int electionCandidateTimeout;

    /**
     * Started after sending an askLeaderRequest. If the node doesn't receive a
     * response, it starts an election.
     */
    private final int askLeaderTimeout;

    /**
     * @param eventsQueue The queue where timeout events should be published.
     */
    public RaftTimeoutManager(LinkedBlockingQueue<Message> eventsQueue)
    {
        timeoutChecker = new TimeoutChecker(eventsQueue);

        // Init timeout intervals
        Random rand = new Random();
        final int randTimeout = rand.nextInt(300, 1800);

        electionCandidateTimeout = 5000;
        electionFollowerTimeout = (randTimeout*2) + 500 + electionCandidateTimeout;
        askLeaderTimeout = electionFollowerTimeout;
        leaderDisconnectedTimeout = randTimeout + 500;
    }

    /**
     * Checks if the election process took too much time. In that case
     * the election is aborted.
     */
    public void startElectionTimeoutCandidate()
    {
        stopTimeout();

        timeoutChecker.startNewTimeout(electionCandidateTimeout, new Message(MessageType.ELECTION_OUT_OF_TIME_CANDIDATE), TimeoutChecker.Mode.EXPLICIT);
    }

    /**
     * Checks if the election process of another node (the candidate node)
     * takes too much time, in which case it might have failed. In that case
     * the node tries to start another election.
     */
    public void startElectionTimeoutFollower()
    {
        stopTimeout();

        timeoutChecker.startNewTimeout(electionFollowerTimeout, new Message(MessageType.ELECTION_OUT_OF_TIME_FOLLOWER), TimeoutChecker.Mode.EXPLICIT);
    }

    /**
     * This timeout is started when the leader disconnection event is received.
     * In that case we start a random timeout, the first node to trigger that
     * timeout starts an election. The others should receive the vote request,
     * stop their timeout checkers and vote for the candidate.
     */
    public void startLeaderDisconnectedTimeout()
    {
        stopTimeout();

        timeoutChecker.startNewTimeout(leaderDisconnectedTimeout, new Message(MessageType.START_ELECTION), TimeoutChecker.Mode.EXPLICIT);
    }

    /**
     * This timeout is started when a node is joining an already running
     * network. It sends a AskLeaderRequest message to all active brokers,
     * then it waits for a response.
     */
    public void startAskLeaderRequestTimeout()
    {
        stopTimeout();

        timeoutChecker.startNewTimeout(askLeaderTimeout, new Message(MessageType.LEADER_DISCONNECTED), TimeoutChecker.Mode.EXPLICIT);
    }

    /**
     * Stop the current running timeout. If no
     * timeouts were running nothing happens.
     */
    public void stopTimeout()
    {
        timeoutChecker.disableAndRemove();
    }

}
