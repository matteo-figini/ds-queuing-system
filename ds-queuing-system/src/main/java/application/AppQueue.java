package application;

import application.exceptions.EndOfQueueException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents the queue of the application.
 *
 * It is an append only queue. Multiple clients can read from it,
 * the reading progress is saved for each client, so that they can
 * continue reading from where they left.
 *
 * It can be created empty or reconstructed from the raft log.
 */
public class AppQueue {
    private final String queueName;

    private ArrayList<Integer> queue = new ArrayList<>();

    private HashMap<String, Integer> mapIndexes = new HashMap<>();

    /**
     * Construct an empty queue.
     * @param queueName The name of the queue to be created.
     */
    public AppQueue(String queueName)
    {
        this.queueName = queueName;
    }

    /**
     * Insert the value at the end of the queue.
     * @param value The value to be inserted.
     */
    public void add(final Integer value)
    {
        queue.add(value);
    }

    /**
     * Read a value from the queue. The read is ordered, each reader gets the value after
     * the one he last red.
     *
     * @param readerName The name of the reader.
     * @return The value from the queue.
     * @throws EndOfQueueException Thrown when the reader reaches the end of the queue.
     */
    public List<Integer> get(final String readerName) throws EndOfQueueException
    {
        ArrayList<Integer> retValue = new ArrayList<>();

        // Get last index if this is not the first read for the user
        Integer idx = mapIndexes.getOrDefault(readerName, 0);

        if (idx >= queue.size())
        {
            // This reader has already reached the end of the list
            throw new EndOfQueueException();
        }

        // Read the value
        while(idx < queue.size())
        {
            retValue.add(queue.get(idx));
            idx++;
        }

        // Update the iterators list
        mapIndexes.put(readerName, idx);

        return retValue;
    }

    /**
     * Used to try the operation without actually doing it. It
     * is useful to see if some exceptions are raised or if the
     * operation is legal.
     * @param readerName The name of the reader.
     * @throws EndOfQueueException Thrown if the reader would reach the end of the queue.
     */
    public void tryGet(final String readerName) throws EndOfQueueException
    {
        // Get last index if this is not the first read for the user
        final Integer idx = mapIndexes.getOrDefault(readerName, 0);

        // Read the value
        if(idx >= queue.size())
        {
            // This reader has already reached the end of the list
            throw new EndOfQueueException();
        }
    }

    /**
     * Queue name getter.
     * @return The name of the queue.
     */
    public String getName()
    {
        return queueName;
    }

    @Override public String toString()
    {
        // TODO: maybe make it fancier

        // Add the queue
        String ret = queueName + ": " + queue.toString();
        ret += "\n";

        // Add the indexes of each client
        for(String clientName : mapIndexes.keySet())
        {
            Integer idx = mapIndexes.get(clientName);
            ret += clientName + ":" + idx + "; ";
        }

        ret += "\n-----------";

        return ret;
    }

}
