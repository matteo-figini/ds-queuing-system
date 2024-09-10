package client;

import java.util.ArrayList;
import java.util.Random;

/**
 * Commands generator, used for the automated
 * client.
 */
public class CommandGenerator {

    private Random random = new Random();

    /**
     * @param availableQueues The list of queues available for commands.
     * @return A random generated command.
     */
    public String generateCommand(final ArrayList<String> availableQueues)
    {
        if(availableQueues.isEmpty())
        {
            // no lists available, first create one
            return generateCreate(availableQueues);
        }

        final int i = random.nextInt(0,100);

        /**
         * 10 % create queue
         * 30 % read queue
         * 60 % append queue
         */
        if(i < 10) return generateCreate(availableQueues);
        else if (i < 40) return generateRead(availableQueues);
        else return generateAppend(availableQueues);
    }

    private String generateCreate(final ArrayList<String> availableQueues)
    {
        boolean nameIsValid = false;
        String queueName = "";

        while(!nameIsValid)
        {
            final int queueNumber = random.nextInt(0, 5000);
            queueName = "q" + queueNumber;

            nameIsValid = !availableQueues.contains(queueName);
        }

        return "create " + queueName;
    }

    private String generateRead(final ArrayList<String> availableQueues)
    {
        // Get a random queue from the list
        final String queueName = availableQueues.get(random.nextInt(0, availableQueues.size()));

        return "read " + queueName;
    }

    private String generateAppend(final ArrayList<String> availableQueues)
    {
        String command = "append ";

        // Get queue name
        command += availableQueues.get(random.nextInt(0, availableQueues.size())) + " ";

        final int SIZE = random.nextInt(1, 5);
        for(int i = 0; i < SIZE; ++i)
        {
            command += random.nextInt(0, 100) + " ";
        }

        return command;
    }
}
