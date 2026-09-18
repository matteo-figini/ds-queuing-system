package application.operations;

import application.Operation;
import application.OperationType;

import java.sql.Timestamp;

public class ReadQueue extends Operation {

    public final String readerName;
    public final String queueName;

    public ReadQueue(final String queueName, final String clientName, final Timestamp ts)
    {
        super(OperationType.READ_QUEUE, clientName, ts);

        this.queueName = queueName;
        this.readerName = clientName;
    }
}
