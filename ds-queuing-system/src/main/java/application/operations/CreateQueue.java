package application.operations;

import application.Operation;
import application.OperationType;

import java.sql.Timestamp;

public class CreateQueue extends Operation {
    public final String queueName;

    public CreateQueue(final String queueName, final String clientName, final Timestamp ts)
    {
        super(OperationType.CREATE_QUEUE, clientName, ts);

        this.queueName = queueName;
    }
}
