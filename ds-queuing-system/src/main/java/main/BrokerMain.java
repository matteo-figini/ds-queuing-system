package main;

import broker.BrokerController;
import broker.BrokerNetwork;

import java.net.*;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles the instantiation of a single broker.
 */
public class BrokerMain {
    public static int MIN_PORT = 1024;
    public static int MAX_PORT = 65535;

    public static void main(String[] args) {
        // Ask the user the address and the port of the locator
        String locatorIPAddress = askIPAddress();
        int locatorPort = askPort("Insert the port of the locator: ");
        String brokerName = askBrokerName();
        int publicPort = askPort("Insert the public port on which the broker will listen to new connections: ");
        String personalIPAddress = retrieveAutomaticallyIPAddress();

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

    /**
     * Retrieve the local IP address found by the {@code InetAddress} class.
     * It is possible to change the IP address in case the local IP address belongs to another network interface.
     * @return The IP address chosen to be visible from outside.
     */
    private static String retrieveManuallyIPAddress () {
        String localIPAddress, alternativeIPAddress;
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
     * Retrieve automatically the local IP address found by the {@code InetAddress} class.
     * @return The IP address chosen to be visible from outside.
     */
    private static String retrieveAutomaticallyIPAddress () {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback())
                    continue;
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLinkLocalAddress())
                        continue;
                    if (address.isSiteLocalAddress())
                        return address.getHostAddress();
                }
            }
        } catch (SocketException e) {
            System.out.println("[EXCEPTION] " + e.getMessage());
        }
        return null;
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
     * Port must be included in the range [1024, 65535] to be valid.
     * @param port The port required to be checked.
     * @return {@code true} if the port is valid, {@code false} otherwise.
     */
    public static boolean isValidPort (int port) {
        return (port >= MIN_PORT && port <= MAX_PORT);
    }
}
