package main;

import broker.BrokerController;
import broker.BrokerNetwork;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles the instantiation of a single broker.
 */
public class BrokerMain {
    public static void main(String[] args) {
        // Ask the user the address and the port of the locator
        String locatorIPAddress = askIPAddress("Insert the IP address of the locator: ");
        int locatorPort = askPort("Insert the port of the locator: ");
        String brokerName = askString("Insert the broker name: ");
        int publicPort = askPort("Insert the public port on which the broker will listen to new connections: ");
        String personalIPAddress = retrieveIPAddress();

        BrokerController brokerController = new BrokerController(brokerName);
        BrokerNetwork brokerNetwork = new BrokerNetwork(brokerController, locatorIPAddress, locatorPort, publicPort);
        brokerController.setBrokerNetwork(brokerNetwork);
        brokerController.startCommunicationGreetings(personalIPAddress);
    }

    /**
     * Takes in input a generic value for the IP address and checks that it is valid.
     * @param outMessage Command message to be printed.
     * @return The inserted IP address.
     */
    private static String askIPAddress (String outMessage) {
        Scanner scanner = new Scanner(System.in);
        String ipAddress;
        do {
            System.out.print(outMessage);
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
     * Takes in input a generic string from stdin.
     * @param outMessage Command message to be printed.
     * @return The inserted string.
     */
    private static String askString (String outMessage) {
        Scanner scanner = new Scanner(System.in);
        System.out.print(outMessage);
        return scanner.nextLine();
    }

    /**
     * Retrieve the local IP address found by the {@code Inet4Address} class. It is possible to change the IP address
     * in case the local IP address belongs to another network interface.
     * @return The IP address chosen to be visible from outside.
     */
    private static String retrieveIPAddress () {
        String localIPAddress, alternativeIPAddress = null;
        Scanner scanner = new Scanner(System.in);
        try {
            localIPAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            localIPAddress = "127.0.0.1";
        }
        System.out.print("[INFO] Proposed local IP Address: " + localIPAddress + ". Insert another IP address (or press ENTER to confirm): ");
        alternativeIPAddress = scanner.nextLine();
        if (alternativeIPAddress != null && !alternativeIPAddress.equalsIgnoreCase("")) {
            localIPAddress = alternativeIPAddress;
        }
        System.out.println("[INFO] Local IP Address: " + localIPAddress);
        return localIPAddress;
    }

    /**
     * Returns true if the IP address specified as parameter is a valid IP address, otherwise it returns false.
     * @param ipAddress The IP address required to be checked.
     * @return {@code true} if the IP address is valid, {@code false} otherwise.
     */
    public static boolean isValidIPAddress (String ipAddress) {
        if (ipAddress == null)
            return false;
        final String IPV4_REGEX_VALIDATOR = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX_VALIDATOR);
        final Matcher matcher = IPV4_PATTERN.matcher(ipAddress);
        return matcher.matches();
    }

    /**
     * Returns true if the port specified as parameter is a valid port, otherwise it returns false.
     * Port must be included in the range [1024, 65536) to be valid.
     * @param port The port required to be checked.
     * @return {@code true} if the port is valid, {@code false} otherwise.
     */
    public static boolean isValidPort (int port) {
        return (port >= 1024 && port < 65536);
    }
}
