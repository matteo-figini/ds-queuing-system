package messages.application;

import messages.Message;
import messages.MessageType;

public class ReadQueueRequest extends Message {
    private final String queueName;

    public ReadQueueRequest(String queueName) {
        super(MessageType.READ_QUEUE_REQUEST);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public String toString() {
        return "ReadQueueRequest{" +
                "queueName='" + queueName + '\'' +
                '}';
    }
}
