package Raft;

/**
 * This class represents an item saved inside the log of a node.
 */
public class LogItem<T> {
    public int term;

    T msg;

    LogItem(T msg, int term)
    {
        this.msg = msg;
        this.term = term;
    }
}
