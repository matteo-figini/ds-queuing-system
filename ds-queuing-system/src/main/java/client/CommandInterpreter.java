package client;

import messages.application.AppendQueueRequest;
import messages.application.CreateQueueRequest;
import messages.application.ReadQueueRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class contains the methods used for processing and interpreting the commands received as inputs from the user.
 */
public class CommandInterpreter {
    private final ClientController clientControllerRef;
    private final String clientName;

    /**
     * Creates the instance of {@code CommandInterpreter}.
     * @param clientControllerRef Reference to the current {@code ClientController}.
     */
    public CommandInterpreter (ClientController clientControllerRef, String clientName) {
        this.clientControllerRef = clientControllerRef;
        this.clientName = clientName;
    }

    /**
     * Receive in input a command from the standard input and process it, based on the format of the command.
     * The command is then processed, according to the specific type.
     * @param command The command received from the standard input.
     */
    public void interpretAndSendCommand (@NotNull String command) {
        String[] commandParts = command.split(" ");
        if (commandParts.length < 1) {
            return;
        }
        if (commandParts[0].equalsIgnoreCase("create")) interpretCreateCommand(commandParts);
        else if (commandParts[0].equalsIgnoreCase("append")) interpretAppendCommand(commandParts);
        else if (commandParts[0].equalsIgnoreCase("read")) interpretReadCommand(commandParts);
        else System.out.println("[ERROR] Unknown command.");
    }

    /**
     * Format the command string to create a new queue in the following format:
     *      create <queue_name>
     * Then, create the {@code CreateQueueRequest} and send the message to the broker's leader.
     * @param commandParts Array containing the words of the command.
     */
    private void interpretCreateCommand (String[] commandParts) {
        if (commandParts.length < 2) return;
        CreateQueueRequest createQueueRequest = new CreateQueueRequest(commandParts[1], clientName);
        System.out.println(createQueueRequest);
        clientControllerRef.sendMessage("leader", createQueueRequest);
    }

    /**
     * Format the command string to append some integers to a queue in the following format:
     *      append <queue_name> <elem1> <elem2> ...
     * Then, create the {@code AppendQueueRequest} and send the message to the broker's leader.
     * @param commandParts Array containing the words of the command.
     */
    private void interpretAppendCommand (String[] commandParts) {
        if (commandParts.length < 2) return;
        List<Integer> elementsToAppend = IntStream.range(2, commandParts.length).mapToObj(i -> Integer.parseInt(commandParts[i])).collect(Collectors.toList());
        // TODO: add client name
        AppendQueueRequest appendQueueRequest = new AppendQueueRequest(commandParts[1], elementsToAppend);
        System.out.println(appendQueueRequest);
        clientControllerRef.sendMessage("leader", appendQueueRequest);
    }

    /**
     * Format the command string to read new elements in a queue in the following format:
     *      read <queue_name>
     * Then, create the {@code ReadQueueRequest} and send the message to the broker's leader.
     * @param commandParts Array containing the words of the command.
     */
    private void interpretReadCommand (String[] commandParts) {
        if (commandParts.length < 2) return;
        // TODO: add client name
        ReadQueueRequest readQueueRequest = new ReadQueueRequest(commandParts[1]);
        System.out.println(readQueueRequest);
        clientControllerRef.sendMessage("leader", readQueueRequest);
    }
}
