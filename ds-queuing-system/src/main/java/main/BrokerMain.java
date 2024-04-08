package main;

import broker.BrokerController;
import broker.BrokerNetwork;

import java.util.Scanner;

/**
 * This class handles the instantiation of a single broker.
 */
public class BrokerMain {
    public static void main(String[] args) {
        // Ask the user the address and the port of the locator
        String locatorIPAddress = askIPAddress();
        int locatorPort = askPort();

        BrokerController brokerController = new BrokerController();
        BrokerNetwork brokerNetwork = new BrokerNetwork(brokerController, locatorIPAddress, locatorPort);
        brokerController.setBrokerNetwork(brokerNetwork);
        /*Thread thread = new Thread(brokerNetwork);
        thread.start();*/
    }

    public static String askIPAddress () {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Insert the locator's IP address: ");
        String ipAddress = scanner.nextLine();
        return ipAddress;
    }

    public static int askPort () {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Insert the locator's port: ");
        int port = Integer.parseInt(scanner.nextLine());
        return port;
    }
}
