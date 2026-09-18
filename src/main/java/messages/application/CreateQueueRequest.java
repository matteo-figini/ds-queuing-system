package messages.application;

import messages.Message;
import messages.MessageType;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * This message is an application message that represents the request of creating a new queue.
 * This request may be sent from the client to the leader and from the leader to the followers.
 */
public class CreateQueueRequest extends OperationRequest {
    private final String queueName;       // Name of the new queue

    /**
     * Create a new message of type {@code CreateQueueRequest}.
     * @param queueName Name of the new queue.
     * @param clientName Name of the client requesting the operation.
     */
    public CreateQueueRequest(String queueName, String clientName) {
        super(MessageType.CREATE_QUEUE_REQUEST, clientName);
        this.queueName = queueName;
    }

    /**
     * @return The name of the new created queue.
     */
    public String getQueueName() {
        return queueName;
    }

    @Override
    public String toString() {
        return "CreateQueueRequest{" +
                "queueName='" + queueName + '\'' +
                ", ts=" + ts +
                '}';
    }
}
