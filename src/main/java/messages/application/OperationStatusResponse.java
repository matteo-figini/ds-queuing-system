package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This message is sent by the leader of the raft network to a client following a
 * OperationStatusRequest message.
 * There are 2 cases:
 * - the operation was lost and now is added to the pending operations.
 * - the operation was already going, no need to worry.
 */
public class OperationStatusResponse extends Message {

    public static final String OPERATION_PENDING = "Operation is in progress, please wait.";
    public static final String OPERATION_ADDED = "The request for this operation was lost, but now we'll take care of it.";
    public static final String OPERATION_COMMITTED = "The request was executed, maybe there were problems with the response message";

    private final String infoMessage;

    private OperationStatusResponse(final String infoMessage)
    {
        super(MessageType.OPERATION_STATUS_RESPONSE);
        this.infoMessage = infoMessage;
    }

    public String getInfoMessage() { return infoMessage; }

    public static OperationStatusResponse newOperationPendingResponse()
    {
        return new OperationStatusResponse(OPERATION_PENDING);
    }

    public static OperationStatusResponse newOperationAddedResponse()
    {
        return new OperationStatusResponse(OPERATION_ADDED);
    }

    public static OperationStatusResponse newOperationCommittedResponse()
    {
        return new OperationStatusResponse(OPERATION_COMMITTED);
    }
}
