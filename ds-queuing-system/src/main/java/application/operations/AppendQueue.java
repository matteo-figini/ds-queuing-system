package application.operations;

import application.Operation;
import application.OperationType;

import java.sql.Timestamp;
import java.util.List;

public class AppendQueue extends Operation {
    public final String queueName;
    public final List<Integer> listValues;

    public AppendQueue(final String queueName, final List<Integer> listValues, final String clientName, final Timestamp ts)
    {
        super(OperationType.APPEND_QUEUE, clientName, ts);

        this.queueName = queueName;
        this.listValues = listValues;
    }
}
