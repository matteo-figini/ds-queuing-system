package application.operations;

import application.Operation;
import application.OperationType;

public class AppendQueue extends Operation {
    public final String queueName;
    public final Integer value;

    public AppendQueue(final String queueName, final Integer value, final Integer operationId)
    {
        super(OperationType.APPEND_QUEUE, operationId);

        this.queueName = queueName;
        this.value = value;
    }
}
