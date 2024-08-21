package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This message is an application message that represents the request of creating a new queue.
 * This request may be sent from the client to the leader and from the leader to the followers.
 */
public class CreateQueueRequest extends Message {
    private final String queueName;       // Name of the new queue
    private final String clientName;

    /**
     * Create a new message of type {@code CreateQueueRequest}.
     * @param queueName Name of the new queue.
     * @param clientName Name of the client requesting the operation.
     */
    public CreateQueueRequest(String queueName, String clientName) {
        super(MessageType.CREATE_QUEUE_REQUEST);
        this.queueName = queueName;
        this.clientName = clientName;
    }

    /**
     * @return The name of the new created queue.
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * @return The name of the client requesting the operation.
     */
    public String getClientName() { return clientName; }

    @Override
    public String toString() {
        return "CreateQueueRequest{" +
                "queueName='" + queueName + '\'' +
                '}';
    }
}
