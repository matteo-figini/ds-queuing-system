package application;

/**
 * This is the base class for defining operations that users can perform.
 */
public class Operation {
    private final OperationType type;

    public Operation(final OperationType type)
    {
        this.type = type;
    }

    public OperationType getType()
    {
        return type;
    }
}
