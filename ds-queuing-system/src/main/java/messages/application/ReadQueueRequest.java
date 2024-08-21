package messages.application;

import messages.Message;
import messages.MessageType;

public class ReadQueueRequest extends Message {
    private final String queueName;
    private final String clientName;

    public ReadQueueRequest(String queueName, String clientName) {
        super(MessageType.READ_QUEUE_REQUEST);
        this.queueName = queueName;
        this.clientName = clientName;
    }

    public String getQueueName() {
        return queueName;
    }

    /**
     * @return The name of the client requesting the operation.
     */
    public String getClientName() { return clientName; }

    @Override
    public String toString() {
        return "ReadQueueRequest{" +
                "queueName='" + queueName + '\'' +
                '}';
    }
}
