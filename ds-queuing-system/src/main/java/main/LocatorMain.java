package main;

import locator.LocatorController;
import locator.LocatorNetwork;

import java.util.Scanner;

/**
 * This class handles the initialization of the {@code LocatorController} and the {@code LocatorNetwork}.
 */
public class LocatorMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Locator's port: ");
        int locatorPort = Integer.parseInt(scanner.nextLine());

        LocatorController locatorController = new LocatorController();
        LocatorNetwork locatorNetwork = new LocatorNetwork(locatorController, locatorPort);
        locatorController.setLocatorNetwork(locatorNetwork);
        Thread thread = new Thread(locatorNetwork);
        thread.start();
    }
}
