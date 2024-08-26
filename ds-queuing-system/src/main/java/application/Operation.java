package application;

import java.io.Serializable;

/**
 * This is the base class for defining operations that users can perform.
 */
public class Operation implements Serializable {
    private final OperationType type;
    private final String clientName;

    public Operation(final OperationType type, final String clientName)
    {
        this.type = type;
        this.clientName = clientName;
    }

    public OperationType getType()
    {
        return type;
    }

    public String getClientName() { return clientName; }

    public String toString()
    {
//        return "type:" + type.name() + "; id:" + id.toString();
        return "type:" + type.name() + "; client:" + clientName;
    }
}
