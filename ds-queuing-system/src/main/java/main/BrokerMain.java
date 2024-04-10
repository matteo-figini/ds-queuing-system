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
        String brokerName = askBrokerName();

        BrokerController brokerController = new BrokerController(brokerName);
        BrokerNetwork brokerNetwork = new BrokerNetwork(brokerController, locatorIPAddress, locatorPort);
        brokerController.setBrokerNetwork(brokerNetwork);
        brokerController.startCommunicationGreetings();
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

    public static String askBrokerName () {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert the broker's name: ");
        return scanner.nextLine();
    }
}
