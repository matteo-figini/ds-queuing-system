package messages.application;

import messages.Message;
import messages.MessageType;

import java.util.ArrayList;
import java.util.List;

public class ReadQueueResponse extends Message {
    private final List<Integer> elementsRead;
    private final boolean status;
    private String infoMessage;

    public ReadQueueResponse (List<Integer> elementsRead, boolean status) {
        super(MessageType.READ_QUEUE_RESPONSE);
        this.elementsRead = elementsRead;
        this.status = status;
    }

    public ReadQueueResponse (List<Integer> elementsRead, boolean status, String infoMessage) {
        this(elementsRead, status);
        this.infoMessage = infoMessage;
    }

    public List<Integer> getElementsRead() {
        return elementsRead;
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
                "elementsRead=" + elementsRead +
                ", status=" + status +
                ", infoMessage='" + infoMessage + '\'' +
                '}';
    }
}
