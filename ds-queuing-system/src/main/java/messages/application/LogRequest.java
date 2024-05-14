package messages.application;

import messages.Message;
import messages.MessageType;
import raft.LogItem;

import java.util.List;

/**
 * This class represents a log update message. The leader sends this message to the followers
 * whenever there are new entries to be added to the log.
 */
public class LogRequest<T> extends Message {

    public final String leaderId;
    public final Integer term;
    public final Integer prefixLen;
    public final Integer prefixTerm;
    public final Integer leaderCommit;
    public final List<LogItem<T>> suffix;

    /**
     * Constructor of a vote request message.
     *
     * @param leaderId The id of the leader, sender of the message.
     * @param term The current term.
     * @param prefixLen The length of the follower's log up to this message (number of acked messages
     *                  by the follower).
     * @param prefixTerm The term of the last log entry in the prefix log.
     * @param leaderCommit Number of log entries committed by the leader.
     * @param suffix The actual entries to be added to the log.
     */
    public LogRequest(String leaderId, Integer term, Integer prefixLen, Integer prefixTerm,
                      Integer leaderCommit, List<LogItem<T>> suffix)
    {
        super(MessageType.LOG_REQUEST);

        this.leaderId = leaderId;
        this.term = term;
        this.prefixLen = prefixLen;
        this.prefixTerm = prefixTerm;
        this.leaderCommit = leaderCommit;
        this.suffix = suffix;
    }
}
