package raft;

/**
 * This class represents an item saved inside the log of a node.
 */
public class LogItem<T> {
    public Integer term;

    T msg;

    LogItem(T msg, Integer term)
    {
        this.msg = msg;
        this.term = term;
    }
}
