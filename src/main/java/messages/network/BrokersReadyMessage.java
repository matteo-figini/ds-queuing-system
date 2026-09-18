package messages.network;

import messages.Message;
import messages.MessageType;

public class BrokersReadyMessage extends Message {

    public BrokersReadyMessage() {
        super(MessageType.BROKERS_READY_MESSAGE);
    }
}
