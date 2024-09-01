package misc;

import java.net.*;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains a set of static variables (constants) and methods that provide utilities for the locator, the
 * brokers and the clients to handle the connection procedures.
 */
public class NetworkUtils {
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

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

    /**
     * Retrieve the local IP address found by the {@code InetAddress} class.
     * It is possible to change the IP address in case the local IP address belongs to another network interface.
     * @return The IP address chosen to be visible from outside.
     */
    public static String retrieveManuallyIPAddress () {
        String localIPAddress, alternativeIPAddress;
        Scanner scanner = new Scanner(System.in);
        localIPAddress = retrieveAutomaticallyIPAddress();
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
    public static String retrieveAutomaticallyIPAddress () {
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
}
