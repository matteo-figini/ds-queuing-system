package application;

import java.io.Serializable;

/**
 * This is the base class for defining operations that users can perform.
 */
public class Operation implements Serializable {
    private final OperationType type;
    private final Integer id;

    public Operation(final OperationType type, final Integer id)
    {
        this.type = type;
        this.id = id;
    }

    public OperationType getType()
    {
        return type;
    }

    public Integer getId() { return id; }

    public String toString()
    {
        return "type:" + type.name() + "; id:" + id.toString();
    }
}
