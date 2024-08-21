package raft;

import application.Operation;

import java.io.Serializable;

/**
 * This class represents an item saved inside the log of a node.
 */
public class LogItem implements Serializable {
    public final Integer term;

    public final Operation msg;

    LogItem(final Operation msg, final Integer term)
    {
        this.msg = msg;
        this.term = term;
    }
}
