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
    private final String clientName;

    /**
     * Create a new message of type {@code AppendQueueRequest}.
     * @param clientName The name of the client requesting the operation.
     * @param queueName Name of the queue to which append the new data.
     * @param newElements List of the elements that will be appended to the queue.
     */
    public AppendQueueRequest(String clientName, String queueName, List<Integer> newElements) {
        super(MessageType.APPEND_QUEUE_REQUEST);
        this.queueName = queueName;
        this.appendElements = new ArrayList<>(newElements);
        this.clientName = clientName;
    }

    /**
     * @return Name of the queue to which append the new data.
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * @return List of the elements that will be appended to the queue.
     */
    public List<Integer> getAppendElements() {
        return appendElements;
    }

    /**
     * @return The name of the client requesting the operation.
     */
    public String getClientName() { return clientName; }

    @Override
    public String toString() {
        return "AppendQueueRequest{" +
                "queueName='" + queueName + '\'' +
                ", appendElements=" + appendElements +
                '}';
    }
}
