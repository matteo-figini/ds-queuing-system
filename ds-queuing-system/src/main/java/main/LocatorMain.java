package main;

import locator.LocatorController;
import locator.LocatorNetwork;

import java.util.Scanner;

/**
 * This class handles the initialization of the {@code LocatorController} and the {@code LocatorNetwork}.
 * The locator is instantiated and then it requires to the user:
 * - The port on which the brokers will connect to the locator.
 * - The number of the brokers in the network.
 */
public class LocatorMain {
    public static void main(String[] args) {
        int brokers;
        Scanner scanner = new Scanner(System.in);

        // Insert the port on which the locator is listening to
        System.out.println("Locator's port: ");
        int locatorPort = Integer.parseInt(scanner.nextLine());

        // Insert the number of brokers allowed in the network
        do {
            System.out.println("Number of brokers in the network: ");
            brokers = Integer.parseInt(scanner.nextLine());
        } while (brokers <= 1 || brokers % 2 == 0);

        LocatorController locatorController = new LocatorController(brokers);
        LocatorNetwork locatorNetwork = new LocatorNetwork(locatorController, locatorPort);
        locatorController.setLocatorNetwork(locatorNetwork);
        Thread thread = new Thread(locatorNetwork);
        thread.start();
    }
}
