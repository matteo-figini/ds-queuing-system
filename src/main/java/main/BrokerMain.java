package main;

import broker.BrokerController;
import broker.BrokerNetwork;

import java.util.Scanner;

import static misc.NetworkUtils.*;

/**
 * This class handles the instantiation of a single broker.
 */
public class BrokerMain {

    public static void main(String[] args) {
        // Ask the user the address and the port of the locator
        String locatorIPAddress = askIPAddress();
        int locatorPort = askPort("Insert the port of the locator: ");
        String brokerName = askBrokerName();
        int publicPort = askPort("Insert the public port on which the broker will listen to new connections: ");
        String personalIPAddress = retrieveIPAddress();

        BrokerController brokerController = new BrokerController(brokerName, personalIPAddress);
        BrokerNetwork brokerNetwork = new BrokerNetwork(brokerController, locatorIPAddress, locatorPort, publicPort);
        brokerController.setBrokerNetwork(brokerNetwork);
        brokerController.startCommunicationGreetings();
    }

    /**
     * Takes in input a generic value for the IP address and checks that it is valid.
     *
     * @return The inserted IP address.
     */
    private static String askIPAddress () {
        Scanner scanner = new Scanner(System.in);
        String ipAddress;
        do {
            System.out.print("Insert the IP address of the locator: ");
            ipAddress = scanner.nextLine();
        } while (!isValidIPAddress(ipAddress));
        return ipAddress;
    }

    /**
     * Takes in input a generic value for the port and checks that it is correct.
     * @param outMessage Command message to be printed.
     * @return The inserted port.
     */
    private static int askPort (String outMessage) {
        Scanner scanner = new Scanner(System.in);
        int port;
        do {
            System.out.print(outMessage);
            port = Integer.parseInt(scanner.nextLine());
        } while (!isValidPort(port));
        return port;
    }

    /**
     * Takes in input a generic string from stdin that represents the proposed broker name.
     * @return The inserted string.
     */
    private static String askBrokerName() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert the broker name: ");
        return scanner.nextLine();
    }
}
