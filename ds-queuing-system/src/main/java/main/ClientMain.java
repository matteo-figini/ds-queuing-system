package main;

import client.ClientController;
import client.ClientNetwork;

import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        String locatorIPAddress = askIPAddress();
        int locatorPort = askPort();
        String clientName = askClientName();

        ClientController clientController = new ClientController(clientName);
        ClientNetwork clientNetwork = new ClientNetwork(clientController, locatorIPAddress, locatorPort);
        clientController.setClientNetwork(clientNetwork);
        clientController.startCommunicationGreetings();
    }

    public static String askIPAddress () {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert the locator's IP address: ");
        return scanner.nextLine();
    }

    public static int askPort () {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert the locator's port: ");
        return Integer.parseInt(scanner.nextLine());
    }

    public static String askClientName() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert the client's name: ");
        return scanner.nextLine();
    }
}
