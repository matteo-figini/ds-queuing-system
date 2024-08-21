package messages.raft;

import application.Operation;
import messages.Message;
import messages.MessageType;

public class RaftAppendMessage extends Message {
    public final Operation operation;
    public RaftAppendMessage(final Operation op)
    {
        super(MessageType.RAFT_APPEND_MESSAGE);

        this.operation = op;
    }
}
