package messages.application;

import messages.Message;
import messages.MessageType;

public class AppendQueueResponse extends Message {
    private final boolean status;
    private String infoMessage;

    public AppendQueueResponse (boolean status) {
        super(MessageType.APPEND_QUEUE_RESPONSE);
        this.status = status;
    }

    public AppendQueueResponse (boolean status, String infoMessage) {
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
        return "AppendQueueResponse{" +
                "status=" + status +
                ", infoMessage='" + infoMessage + '\'' +
                '}';
    }
}
