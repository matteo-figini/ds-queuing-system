package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This class represents a vote response message, sent by a follower to a candidate
 * as reply to a VoteRequest message.
 */
public class VoteResponse extends Message {

    public final String voterId;

    public final Integer voterCurrentTerm;

    public final boolean vote;

    /**
     * Constructor of a vote response message.
     *
     * @param voterId Id of the voter node.
     * @param voterCurrentTerm Current term of the voter node.
     * @param vote True if the vote is granted to the candidate, false otherwise.
     */
    public VoteResponse(String voterId, Integer voterCurrentTerm, boolean vote)
    {
        super(MessageType.VOTE_RESPONSE);

        this.voterId = voterId;
        this.voterCurrentTerm = voterCurrentTerm;
        this.vote = vote;
    }
}
