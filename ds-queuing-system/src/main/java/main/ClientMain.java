package main;

import client.ClientController;
import client.ClientNetwork;

import java.util.Scanner;

import static misc.NetworkUtils.isValidIPAddress;
import static misc.NetworkUtils.isValidPort;

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
        String ipAddress;
        do {
            System.out.print("Insert the locator's IP address: ");
            ipAddress = scanner.nextLine();
        } while (!isValidIPAddress(ipAddress));
        return ipAddress;
    }

    /**
     * Takes in input a generic value for the port and checks that it is correct.
     *
     * @return The inserted port.
     */
    private static int askPort () {
        Scanner scanner = new Scanner(System.in);
        int port;
        do {
            System.out.print("Insert the locator's port: ");
            port = Integer.parseInt(scanner.nextLine());
        } while (!isValidPort(port));
        return port;
    }

    public static String askClientName() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert the client's name: ");
        return scanner.nextLine();
    }
}
