package application.operations;

import application.Operation;
import application.OperationType;

public class ReadQueue extends Operation {

    public final String readerName;
    public final String queueName;

    public ReadQueue(final String readerName, final String queueName, final Integer operationId)
    {
        super(OperationType.READ_QUEUE, operationId);

        this.queueName = queueName;
        this.readerName = readerName;
    }
}
