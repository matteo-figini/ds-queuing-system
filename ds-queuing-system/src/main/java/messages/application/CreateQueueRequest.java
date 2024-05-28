package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This message is an application message that represents the request of creating a new queue.
 * This request may be sent from the client to the leader and from the leader to the followers.
 */
public class CreateQueueRequest extends Message {
    private final String queueName;       // Name of the new queue

    /**
     * Create a new message of type "CreateQueueRequest".
     * @param queueName Name of the new queue.
     */
    public CreateQueueRequest(String queueName) {
        super(MessageType.CREATE_QUEUE_REQUEST);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public String toString() {
        return "CreateQueueRequest{" +
                "queueName='" + queueName + '\'' +
                '}';
    }
}
