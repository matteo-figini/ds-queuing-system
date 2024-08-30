package messages.application;

import messages.Message;
import messages.MessageType;

import java.sql.Timestamp;
import java.time.Instant;

public class ReadQueueRequest extends OperationRequest {
    private final String queueName;

    public ReadQueueRequest(String queueName, String clientName) {
        super(MessageType.READ_QUEUE_REQUEST, clientName);
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public String toString() {
        return "ReadQueueRequest{" +
                "queueName='" + queueName + '\'' +
                ", ts=" + ts +
                '}';
    }
}
