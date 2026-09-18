package application;

public enum OperationType {
    /** Read from a queue. */
    READ_QUEUE,
    /** Write in a queue. */
    APPEND_QUEUE,
    /** Create a new queue. */
    CREATE_QUEUE,
}
