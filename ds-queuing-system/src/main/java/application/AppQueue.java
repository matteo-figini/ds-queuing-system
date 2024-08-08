package application;

import application.exceptions.EndOfQueueException;

import java.util.ArrayList;
import java.util.HashMap;

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
    private final Object mutexQueue = new Object();

    private HashMap<String, Integer> mapIndexes = new HashMap<>();
    private final Object mutexMapIndexes = new Object();

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
        synchronized (mutexQueue)
        {
            queue.add(value);
        }
    }

    /**
     * Read a value from the queue. The read is ordered, each reader gets the value after
     * the one he last red.
     *
     * @param readerName The name of the reader.
     * @return The value from the queue.
     * @throws EndOfQueueException Thrown when the reader reaches the end of the queue.
     */
    public Integer get(final String readerName) throws EndOfQueueException
    {
        // TODO: this function could probably be improved by using
        //  ReentrantReadWriteLock to grant access. This is the safest
        //  and simplest implementation.

        Integer idx = 0;
        Integer retValue = 0;

        synchronized (mutexMapIndexes)
        {
            // Get last index if this is not the first read for the user
            if(mapIndexes.containsKey(readerName))
            {
                idx = mapIndexes.get(readerName);
            }
            else
            {
                idx = 0;
            }


            // Read the value
            synchronized (mutexQueue)
            {
                if(idx < queue.size())
                {
                    retValue = queue.get(idx);
                    idx++;
                }
                else
                {
                    // This reader has already reached the end of the list
                    throw new EndOfQueueException("Reached the end of the queue");
                }
            }


            // Update the iterators list
            mapIndexes.put(readerName, idx);
        }

        return retValue;
    }

    /**
     * Queue name getter.
     * @return The name of the queue.
     */
    public String getName()
    {
        return queueName;
    }

}
