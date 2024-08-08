package application.operations;

import application.Operation;
import application.OperationType;

public class CreateQueue extends Operation {
    public final String queueName;

    public CreateQueue(final String queueName)
    {
        super(OperationType.CREATE_QUEUE);

        this.queueName = queueName;
    }
}
