package application.operations;

import application.Operation;
import application.OperationType;

public class CreateQueue extends Operation {
    public final String queueName;

    public CreateQueue(final String queueName, final String clientName)
    {
        super(OperationType.CREATE_QUEUE, clientName);

        this.queueName = queueName;
    }
}
