package misc;

/**
 * This enumeration defines the possible states in which the network can be.
 * {@code CONNECTING_BROKERS}: the brokers are still connecting for the first time.
 * {@code NETWORK_CONNECTED}: all the brokers are connected and they are ready to start.
 */
public enum NetworkState {
    CONNECTING_BROKERS,
    NETWORK_CONNECTED
}
