package main;

import client.ClientController;
import client.ClientNetwork;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientMain {
    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

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
        return (port >= MIN_PORT && port <= MAX_PORT);
    }
}
