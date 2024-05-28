package messages.application;

import messages.Message;
import messages.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * This message is sent whenever a client wants to append new elements to an already created queue.
 * It contains the name of the selected queue and the list of elements (integers) to be appended
 * to that queue.
 */
public class AppendQueueRequest extends Message {
    private final String queueName;
    private final List<Integer> appendElements;

    public AppendQueueRequest(String queueName, List<Integer> newElements) {
        super(MessageType.APPEND_QUEUE_REQUEST);
        this.queueName = queueName;
        this.appendElements = new ArrayList<>(newElements);
    }

    public String getQueueName() {
        return queueName;
    }

    public List<Integer> getAppendElements() {
        return appendElements;
    }

    @Override
    public String toString() {
        return "AppendQueueRequest{" +
                "queueName='" + queueName + '\'' +
                ", appendElements=" + appendElements +
                '}';
    }
}
