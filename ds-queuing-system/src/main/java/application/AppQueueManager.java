package application;

import application.exceptions.EndOfQueueException;
import application.exceptions.NameAlreadyUsedException;
import application.exceptions.QueueNotFoundException;
import application.operations.CreateQueue;
import application.operations.ReadQueue;
import application.operations.WriteQueue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class manages all the queues. It is used to access them and perform operations.
 */
public class AppQueueManager {

    /**
     * Map used to store each queue by its name.
     */
    private HashMap<String, AppQueue> mapQueues = new HashMap<>();

    public AppQueueManager() {}

    /**
     * Read a value from the queue. The value depends on the name of the user requesting it,
     * as the queue keeps track of the last red item by every user.
     * @param queueName The name of the queue.
     * @param readerName The name of the user requesting to read.
     * @return The value from the queue.
     * @throws QueueNotFoundException Thrown if there is no queue with such name.
     * @throws EndOfQueueException Thrown if the user reached the end of the queue.
     */
    public Integer read(final String queueName, final String readerName) throws QueueNotFoundException, EndOfQueueException
    {
        if(!mapQueues.containsKey(queueName))
        {
            throw new QueueNotFoundException("There is no queue corresponding to the name given");
        }

        return mapQueues.get(queueName).get(readerName);
    }

    /**
     * Insert a value at the end of the specified queue.
     * @param queueName The name of the queue.
     * @param value The value to be inserted.
     * @throws QueueNotFoundException Thrown if there is no queue with such name.
     */
    public void insert(final String queueName, final Integer value) throws QueueNotFoundException
    {
        if(!mapQueues.containsKey(queueName))
        {
            throw new QueueNotFoundException("There is no queue corresponding to the name given");
        }

        mapQueues.get(queueName).add(value);
    }

    /**
     * Create a new queue.
     * @param queueName The name of the queue.
     * @throws NameAlreadyUsedException Thrown if the name is already used.
     */
    public void create(final String queueName) throws NameAlreadyUsedException
    {
        if(mapQueues.containsKey(queueName))
        {
            throw new NameAlreadyUsedException("There is already a queue using that name");
        }

        AppQueue newQueue = new AppQueue(queueName);
        mapQueues.put(queueName, newQueue);
    }

    /**
     * Used to recreate the queues from the log.
     * @param listOperations The log of the operations.
     */
    public void recreateFromLog(final List<Operation> listOperations)
    {
        // Delete data already present
        mapQueues.clear();

        // Start recreating
        Iterator<Operation> it = listOperations.iterator();
        while(it.hasNext())
        {
            Operation op = it.next();

            switch (op.getType())
            {
                case READ_QUEUE -> recreateRead((ReadQueue) op);
                case WRITE_QUEUE -> recreateWrite((WriteQueue) op);
                case CREATE_QUEUE -> recreateCreate((CreateQueue) op);
            }
        }
    }

    private void recreateRead(ReadQueue op)
    {
        try
        {
            read(op.queueName, op.readerName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("The log is faulty, exception while recreating queues");
        }
    }

    private void recreateWrite(WriteQueue op)
    {
        try
        {
            insert(op.queueName, op.value);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("The log is faulty, exception while recreating queues");
        }
    }

    private void recreateCreate(CreateQueue op)
    {
        try
        {
            create(op.queueName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("The log is faulty, exception while recreating queues");
        }
    }
}
