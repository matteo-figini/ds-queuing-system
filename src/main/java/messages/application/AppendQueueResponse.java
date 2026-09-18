package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This message is sent in response to an {@code AppendQueueRequest}.
 * It contains a variable signaling whether the append operation was successful or not, and possibly a text message
 * that can be sent to specify any kind of information to the user.
 */
public class AppendQueueResponse extends Message {
    private final boolean status;
    private String infoMessage;

    /**
     * Create a message of type {@code AppendQueueResponse}.
     * @param status {@code true} if the append operation was successful, {@code false} otherwise.
     */
    public AppendQueueResponse (boolean status) {
        super(MessageType.APPEND_QUEUE_RESPONSE);
        this.status = status;
    }

    /**
     * Create a message of type {@code AppendQueueResponse}.
     * @param status {@code true} if the append operation was successful, {@code false} otherwise.
     * @param infoMessage Text message to specify details of the append operation.
     */
    public AppendQueueResponse (boolean status, String infoMessage) {
        this(status);
        this.infoMessage = infoMessage;
    }

    /**
     * @return The status of the append operation.
     */
    public boolean getStatus() {
        return status;
    }

    /**
     * @return A message specifying possible details of the append operation.
     */
    public String getInfoMessage() {
        return infoMessage;
    }

    @Override
    public String toString() {
        return "AppendQueueResponse{" +
                "status=" + status +
                ", infoMessage='" + infoMessage + '\'' +
                '}';
    }
}
