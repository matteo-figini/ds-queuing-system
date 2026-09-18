package application;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * This is the base class for defining operations that users can perform.
 */
public class Operation implements Serializable {
    private final OperationType type;
    private final String clientName;

    /**
     * The timestamp at which the operation was requested. NOTE:
     * OPERATION ARE TIME ORDERED ONLY BY CLIENT NAME! Different
     * clients operations MIGHT NOT BE TIME ORDERED!
     */
    private final Timestamp ts;

    /**
     * @param type The type of operation requested.
     * @param clientName The name of the client requesting the operation.
     * @param ts The timestamp at which the operation was requested.
     */
    public Operation(final OperationType type, final String clientName, final Timestamp ts)
    {
        this.type = type;
        this.clientName = clientName;
        this.ts = ts;
    }

    public OperationType getType()
    {
        return type;
    }

    public String getClientName() { return clientName; }

    public Timestamp getTimestamp() { return ts; }

    public String toString()
    {
        return "type:" + type.name() +
                "; client:" + clientName +
                "; ts:" + ts;
    }

    /**
     * @param op2 The operation to be compared to
     * @return True if the operations are equal.
     */
    public boolean equals(final Operation op2)
    {
        if(!type.equals(op2.type)) return false;

        if(!clientName.equals(op2.clientName)) return false;

        return ts.equals(op2.ts);
    }

    /**
     * Checks if this operation FROM THE SAME CLIENT happened
     * before op2.
     *
     * NOTE: OPERATION ARE TIME ORDERED ONLY BY CLIENT NAME! Different
     * clients operations MIGHT NOT BE TIME ORDERED!
     *
     * @param op2 The operation to be compared to.
     * @return True if this operation happened before op2. False otherwise
     * OR IF CLIENTS ARE DIFFERENT.
     */
    public boolean happenedBefore(final Operation op2)
    {
        if(!sameClient(op2))
            return false;

        return ts.before(op2.ts);
    }

    /**
     * @param op2 The operation to be compared to.
     * @return True if both operations where requested
     * by the same client.
     */
    public boolean sameClient(final Operation op2)
    {
        return clientName.equals(op2.clientName);
    }
}
