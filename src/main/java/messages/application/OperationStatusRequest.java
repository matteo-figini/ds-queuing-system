package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This message is sent by the client to the leader of the raft network whenever
 * he thinks that there might be problem with the execution of the request he
 * previously made (and hasn't received a response).
 */
public class OperationStatusRequest extends Message {

    /** The request we are asking info about */
    private final OperationRequest previousRequest;

    public OperationStatusRequest(OperationRequest previousRequest)
    {
        super(MessageType.OPERATION_STATUS_REQUEST);

        this.previousRequest = previousRequest;
    }

    public OperationRequest getRequest() { return previousRequest; }
}
