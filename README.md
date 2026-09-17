# Fault-Tolerant Distributed Queuing System ⚙️

This repository contains the code for the Distributed Systems Course Project (Academic Year 2023/2024), which consists in a distributed, fault-tolerant message queuing system implemented in Java 17. The system uses an implementation of the **Raft Consensus Algorithm** to replicate operations across a cluster of brokers and achieve fault tolerance. A central **Locator** service facilitates peer discovery and dynamic leader routing.

---

## Architecture Overview 🏛️

The architecture consists of three main components:

* **Locator**: A registry coordinating network formation and peer discovery. It validates unique node names, tracks the cluster until the expected odd number of brokers connects, and maintains the reference to the current Raft leader.
* **Broker Cluster**: Replicated storage nodes executing an adapted Raft consensus protocol. Brokers maintain an append-only log backed by local disk storage (`<nodeId>log.dat` and `<nodeId>status.dat`). State machines persist named FIFO queues where multiple readers maintain per-client reading offsets.
* **Client / Automated Client**: Applications that discover the active broker leader via the Locator and issue queue operations. If the leader crashes, clients request updated leader coordinates from the Locator and verify pending transaction status via `OperationStatusRequest`.

```text
                +-------------------+
                |      Locator      |
                +-------------------+
                   ^       ^       ^
     Discovery &   |       |       |  Discovery &
     Leader Info   |       |       |  Leader Info
                   v       |       v
           +----------+    |   +----------+
           |  Client  |    |   |  Client  |
           +----------+    |   +----------+
                 |         |         |
      Commands / |         |         | Commands /
      Operations |         |         | Operations
                 v         v         v
              +-------------------------+
              |    Broker (Leader)      |
              +-------------------------+
                   ^               ^
     Log / Heartbeat|               | Log / Heartbeat
                   v               v
            +------------+   +------------+
            |   Broker   |---|   Broker   |
            | (Follower) |   | (Follower) |
            +------------+   +------------+
                   P2P Broker Mesh
```

---

## Key Features ✨

* **Raft Consensus Engine**: Implements term-based leader election, randomized election timeouts, vote processing, log replication, and quorum commit mechanics.
* **Crash Recovery & Persistence**: `LogFilesHandler` serializes log entries and Raft status variables (`currentTerm`, `votedFor`, `commitLength`) to disk, enabling crashed brokers to reconstruct queues upon reboot.
* **Independent Client Offsets**: Each queue tracks read offsets separately per client, allowing parallel consumption from the same queue without destructive reads.
* **Interactive and Automated Clients**: Includes an interactive CLI client (`ClientMain`) and a scriptable load client (`AutomatedClientMain`) that randomly dispatches read, create, and append operations.

---

## Client Commands 💬

Connected clients interact directly with the active broker leader using the following command syntax:

| Command | Description | Example |
| :--- | :--- | :--- |
| `create <queue_name>` | Creates a new named FIFO queue. | `create tasks` |
| `append <queue_name> <val1> <val2> ...` | Appends one or more integer values to the specified queue. | `append tasks 10 20 30` |
| `read <queue_name>` | Reads all new elements from the queue since the client's last read. | `read tasks` |

---

## Build & Execution 🚀

### Prerequisites
* Java Development Kit (JDK) 17 or higher
* Apache Maven

### Build
Package the shade JARs using Maven:

```bash
mvn clean package
```

The build produces three standalone JARs in the `target/` directory:
* `Locator.jar` (`main.LocatorMain`)
* `Broker.jar` (`main.BrokerMain`)
* `Client.jar` (`main.ClientMain`)

### Startup Sequence

1. **Launch the Locator**:
   ```bash
   java -jar target/Locator.jar
   ```
   Specify the listening port (between 1024 and 65535) and the total number of brokers (must be an odd integer > 1).

2. **Launch the Brokers**:
   Run each broker instance in a separate terminal:
   ```bash
   java -jar target/Broker.jar
   ```
   Provide the Locator IP/port, a unique broker name, and a local listening port. Once all configured brokers join, the cluster initiates election and begins processing.

3. **Launch the Clients**:
   ```bash
   # Interactive CLI client
   java -jar target/Client.jar

   # Or automated client
   java -cp target/Client.jar main.AutomatedClientMain
   ```
   Enter the Locator IP/port and a unique client identifier. The client resolves the leader and is ready for queue operations.
