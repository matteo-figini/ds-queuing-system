package raft;

import messages.Message;

import java.util.concurrent.LinkedBlockingQueue;
import static messages.MessageType.LEADER_HEARTBEAT_NOTIFY;

/**
 * This class is used as a notifier for the leader
 * to send the heartbeat to its followers. Once started
 * it periodically creates LEADER_HEARTBEAT_NOTIFY, that
 * are sent to the leader via the eventsQueue.
 */
public class HeartBeatNotifier {

    private int timeBetweenHeartbeats;
    private LinkedBlockingQueue<Message> eventsQueue;
    private Thread threadNotifier;
    private boolean isRunning = false;

    /**
     * @param eventsQueue The queue where events are published.
     * @param timeBetweenHeartbeats Time between heartbeat notifications in milliseconds.
     */
    public HeartBeatNotifier(LinkedBlockingQueue<Message> eventsQueue, int timeBetweenHeartbeats)
    {
        this.timeBetweenHeartbeats = timeBetweenHeartbeats;
        this.eventsQueue = eventsQueue;
    }

    /**
     * Start the notifier. If already running nothing happens.
     */
    public void start()
    {
        if(isRunning)
        {
            // Already running
            return;
        }

        threadNotifier = new Thread(threadCode);
        threadNotifier.start();
        isRunning = true;
    }

    private final Runnable threadCode = () ->
    {
        while(true)
        {
            try
            {
                Thread.sleep(timeBetweenHeartbeats);
            } catch (Exception e) {}

            eventsQueue.add(new Message(LEADER_HEARTBEAT_NOTIFY));
        }
    };

    public boolean isRunning()
    {
        return isRunning;
    }
}
