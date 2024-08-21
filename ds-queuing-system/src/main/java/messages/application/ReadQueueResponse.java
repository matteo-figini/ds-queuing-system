package messages.application;

import messages.Message;
import messages.MessageType;

import java.util.ArrayList;
import java.util.List;

public class ReadQueueResponse extends Message {
    private final Integer elementRead;
    private final boolean status;
    private String infoMessage;

    public ReadQueueResponse (Integer elementRead, boolean status) {
        super(MessageType.READ_QUEUE_RESPONSE);
        this.elementRead = elementRead;
        this.status = status;
    }

    public ReadQueueResponse (Integer elementRead, boolean status, String infoMessage) {
        this(elementRead, status);
        this.infoMessage = infoMessage;
    }

    public Integer getElementsRead() {
        return elementRead;
    }

    public boolean getStatus() {
        return status;
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    @Override
    public String toString() {
        return "ReadQueueResponse{" +
                "elementsRead=" + elementRead +
                ", status=" + status +
                ", infoMessage='" + infoMessage + '\'' +
                '}';
    }
}
