package messages.application;

import messages.Message;
import messages.MessageType;

/**
 * This class represents a message sent in response as a CreateQueueRequest message.
 * It is composed by a boolean value ("status") that is true if the creation was correctly performed and false if
 * something went wrong during the creation of the queue.
 * It also offers the possibility to write a short message string, especially in case of failed creation.
 */
public class CreateQueueResponse extends Message {
    private final boolean status;
    private String infoMessage;

    /**
     *
     * @param status
     */
    public CreateQueueResponse (boolean status) {
        super(MessageType.CREATE_QUEUE_RESPONSE);
        this.status = status;
    }

    public CreateQueueResponse (boolean status, String infoMessage) {
        this(status);
        this.infoMessage = infoMessage;
    }

    public boolean getStatus() {
        return status;
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    @Override
    public String toString() {
        return "CreateQueueResponse{" +
                "status=" + status +
                ", infoMessage='" + infoMessage + '\'' +
                '}';
    }
}
