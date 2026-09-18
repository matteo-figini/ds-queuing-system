package client;

import messages.application.CreateQueueResponse;
import java.util.ArrayList;

public class AutomatedClientController extends ClientController{

    private CommandGenerator commandGenerator = new CommandGenerator();

    /**
     * List of the queues created (available for commands).
     */
    private ArrayList<String> activeQueues = new ArrayList<>();

    /**
     * Name of the queue in the last queue request (still pending).
     */
    private String lastCreateQueueName = "";

    /**
     * Time between one request and another.
     */
    private final int timeoutCommands;

    public AutomatedClientController(String clientName, int timeoutMillis)
    {
        super(clientName);
        this.timeoutCommands = timeoutMillis;
    }

    @Override
    protected void inputRoutine()
    {
        while (!keyboardInputRoutine.isShutdown()) {
            final String readString = commandGenerator.generateCommand(activeQueues);

            synchronized (mutexConnectedToLeader)
            {
                synchronized (mutexWaitingForOperationResponse)
                {
                    if(connectedToLeader && !waitingForOperationResponse)
                    {
                        commandInterpreter.interpretAndSendCommand(readString);

                        if(readString.startsWith("create"))
                        {
                            // save queue name
                            String[] parts = readString.split(" ");
                            lastCreateQueueName = parts[1];
                        }
                    }
                    else if(!connectedToLeader)
                    {
                        System.out.println("[ERROR] Cannot make requests while not connected to the leader.");
                    }
                    else if(waitingForOperationResponse)
                    {
                        System.out.println("[ERROR] Cannot make new requests while waiting for an operation to complete.");
                    }
                }
            }

            try
            {
                Thread.sleep(timeoutCommands);
            }
            catch (Exception e) {}
        }
    }

    @Override
    protected void onCreateQueueResponse(final CreateQueueResponse response)
    {
        if(response.getStatus())
        {
            // Queue was created, add to available
            activeQueues.add(lastCreateQueueName);
        }

        super.onCreateQueueResponse(response);
    }
}
