package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This class represents a vote request message, sent by a candidate to a follower.
 */
public class VoteRequest extends Message {

    public final String cId;

    public final Integer cTerm;

    public final Integer cLogLength;

    public final Integer cLogLastTerm;

    /**
     * Constructor of a vote request message.
     *
     * @param cId Id of the candidate node.
     * @param cTerm Current term of the candidate node.
     * @param cLogLength Length of the candidate's log.
     * @param cLogLastTerm Term of the last entry in the candidate's log.
     */
    public VoteRequest(String cId, Integer cTerm, Integer cLogLength, Integer cLogLastTerm)
    {
        super(MessageType.VOTE_REQUEST);

        this.cId = cId;
        this.cTerm = cTerm;
        this.cLogLength = cLogLength;
        this.cLogLastTerm = cLogLastTerm;
    }
}
