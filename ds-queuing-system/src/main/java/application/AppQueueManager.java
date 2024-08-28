package application;

import application.exceptions.EndOfQueueException;
import application.exceptions.NameAlreadyUsedException;
import application.exceptions.QueueNotFoundException;
import application.operations.CreateQueue;
import application.operations.ReadQueue;
import application.operations.AppendQueue;

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
    public Integer commitRead(final String queueName, final String readerName) throws QueueNotFoundException, EndOfQueueException
    {
        if(!mapQueues.containsKey(queueName))
        {
            throw new QueueNotFoundException();
        }

        return mapQueues.get(queueName).get(readerName);
    }

    /**
     * Insert a value at the end of the specified queue.
     * @param queueName The name of the queue.
     * @param listValues The list of values to be inserted.
     * @throws QueueNotFoundException Thrown if there is no queue with such name.
     */
    public void commitAppend(final String queueName, final List<Integer> listValues) throws QueueNotFoundException
    {
        if(!mapQueues.containsKey(queueName))
        {
            throw new QueueNotFoundException();
        }

        AppQueue queue = mapQueues.get(queueName);
        for(Integer value : listValues)
        {
            queue.add(value);
        }
    }

    /**
     * Create a new queue.
     * @param queueName The name of the queue.
     * @throws NameAlreadyUsedException Thrown if the name is already used.
     */
    public void commitCreate(final String queueName) throws NameAlreadyUsedException
    {
        if(mapQueues.containsKey(queueName))
        {
            throw new NameAlreadyUsedException();
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
            final Operation op = it.next();
            recreateOperation(op);
        }
    }

    /**
     * Utility used to wrap the "try-catch" needed for the
     * recreateFromLog() function.
     *
     * @param operation The operation to be recreated.
     * @throws RuntimeException Thrown if the operation is invalid.
     */
    private void recreateOperation(final Operation operation)
    {
        try
        {
            switch (operation.getType())
            {
                case READ_QUEUE -> {
                    final ReadQueue op = (ReadQueue) operation;
                    commitRead(op.queueName, op.readerName);
                }
                case APPEND_QUEUE -> {
                    final AppendQueue op = (AppendQueue) operation;
                    commitAppend(op.queueName, op.listValues);
                }
                case CREATE_QUEUE -> {
                    final CreateQueue op = (CreateQueue) operation;
                    commitCreate(op.queueName);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("The log is faulty, exception while recreating queues");
        }
    }

    /**
     * Utility to check if an operation is valid, without actually
     * performing it.
     *
     * @param queueName The name of the queue to be created.
     * @throws NameAlreadyUsedException Thrown if there is already a queue with such name.
     */
    public void tryCreateQueue(final String queueName) throws NameAlreadyUsedException
    {
        if(mapQueues.containsKey(queueName))
        {
            throw new NameAlreadyUsedException();
        }
    }

    /**
     * Utility to check if an operation is valid, without actually
     * performing it.
     *
     * @param queueName The name of the queue.
     * @param readerName The name of the reader.
     * @throws QueueNotFoundException Thrown if there's no queue with such name.
     * @throws EndOfQueueException Thrown if the reader has already reached the end of the queue.
     */
    public void tryReadQueue(final String queueName, final String readerName) throws QueueNotFoundException, EndOfQueueException
    {
        if(!mapQueues.containsKey(queueName))
        {
            throw new QueueNotFoundException();
        }

        mapQueues.get(queueName).tryGet(readerName);
    }

    /**
     * Utility to check if an operation is valid, without actually
     * performing it.
     *
     * @param queueName The name of the queue.
     * @throws QueueNotFoundException Thrown if there's no queue with such name.
     */
    public void tryAppendQueue(final String queueName) throws QueueNotFoundException
    {
        if(!mapQueues.containsKey(queueName))
        {
            throw new QueueNotFoundException();
        }
    }

    /**
     * Utility for showing the state of the queues.
     */
    public void printQueues()
    {
        System.out.println("- Queues: ---------");

        for(String queueName : mapQueues.keySet())
        {
            final AppQueue queue = mapQueues.get(queueName);

            System.out.println(queue.toString());
        }
    }
}
