package application.operations;

import application.Operation;
import application.OperationType;

public class CreateQueue extends Operation {
    public final String queueName;

    public CreateQueue(final String queueName, final Integer operationId)
    {
        super(OperationType.CREATE_QUEUE, operationId);

        this.queueName = queueName;
    }
}
