package locator;

/**
 * This class is a representation of the data structure containing the reference of a specific broker stored
 * in the locator. For each broker, relevant information such as the broker's name, the IP address and the port are
 * stored. Moreover, the class is immutable.
 */
public class BrokerReference {
    private final String brokerName;
    private final String brokerAddress;
    private final int brokerPort;

    public BrokerReference(String brokerName, String brokerAddress, int brokerPort) {
        this.brokerName = brokerName;
        this.brokerAddress = brokerAddress;
        this.brokerPort = brokerPort;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public int getBrokerPort() {
        return brokerPort;
    }
}
