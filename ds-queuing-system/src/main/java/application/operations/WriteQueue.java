package application.operations;

import application.Operation;
import application.OperationType;

public class WriteQueue extends Operation {
    public final String queueName;
    public final Integer value;

    public WriteQueue(final String queueName, final Integer value)
    {
        super(OperationType.WRITE_QUEUE);

        this.queueName = queueName;
        this.value = value;
    }
}
