package messages.application;

import messages.Message;
import messages.MessageType;

import java.sql.Timestamp;
import java.time.Instant;

public class OperationRequest extends Message {

    private final String clientName;
    protected final Timestamp ts = Timestamp.from(Instant.now());

    public OperationRequest(MessageType type, String clientName)
    {
        super(type);
        this.clientName = clientName;
    }

    /**
     * @return The name of the client requesting the operation.
     */
    public String getClientName() { return clientName; }

    /**
     * @return The time at which the request was created.
     */
    public Timestamp getTimestamp() { return ts; }
}
