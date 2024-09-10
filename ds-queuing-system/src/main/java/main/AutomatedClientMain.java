package main;

import client.AutomatedClientController;
import client.ClientController;
import client.ClientNetwork;

import static main.ClientMain.*;

public class AutomatedClientMain {
    public static void main(String[] args) {
        String locatorIPAddress = askIPAddress();
        int locatorPort = askPort();
        String clientName = askClientName();

        AutomatedClientController clientController = new AutomatedClientController(clientName, 5000);
        ClientNetwork clientNetwork = new ClientNetwork(clientController, locatorIPAddress, locatorPort);
        clientController.setClientNetwork(clientNetwork);
        clientController.startCommunicationGreetings();
    }
}
