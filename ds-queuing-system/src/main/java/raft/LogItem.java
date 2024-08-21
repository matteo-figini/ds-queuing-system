package raft;

import application.Operation;

import java.io.Serializable;

/**
 * This class represents an item saved inside the log of a node.
 */
public class LogItem implements Serializable {
    public final Integer term;

    public final Operation operation;

    public LogItem(final Operation op, final Integer term)
    {
        this.operation = op;
        this.term = term;
    }
}
