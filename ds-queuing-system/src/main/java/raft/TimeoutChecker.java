package raft;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is a wrapper around a thread, which is in charge of checking if a timeout is reached
 * before an event occurs.
 * If the event occurs the timeout is reset.
 * If the timeout triggers then a new event (the timeout event) is pushed in the event queue.
 *
 * @param <T> The events class.
 */
public class TimeoutChecker<T>
{
    /**
     * Defines the behaviour of the timeout checker once a timeout
     * event is reached.
     */
    public enum Mode
    {
        /** Stop the timeout procedure once a timeout event is reached. */
        SINGLE,

        /** Keep the procedure going. It might generate multiple events. */
        MULTIPLE,
    }

    /**
     * The queue where events are published, needed to publish the eventual
     * timeout event.
     */
    private LinkedBlockingQueue<T> eventsQueue;

    /**
     * The event to be sent in the queue in case of timeout.
     */
    private T timeoutEvent;

    /**
     * Timeout value in milliseconds.
     */
    private int timeout;

    private Mode mode;

    /**
     * This boolean is set to true when the expected event
     * happened. A new timeout should start.
     */
    private boolean eventReceived = false;
    private final Object mutexEventReceived = new Object();

    /**
     * Put to true to indicate that the thread should stop.
     */
    private boolean stopFlag = false;
    private final Object mutexStopFlag = new Object();

    /**
     * The thread controlling the timeout event.
     */
    private Thread timeoutThread;

    /**
     * The code of the timeout checker thread.
     */
    private final Runnable threadCode = () ->
    {
        boolean keepGoing;
        synchronized (mutexStopFlag)
        {
            keepGoing = !stopFlag;
        }

        while(keepGoing)
        {
            try
            {
                Thread.sleep(timeout);
            } catch (Exception e) {}

            synchronized (mutexStopFlag)
            {
                keepGoing = !stopFlag;
            }

            if(keepGoing)
            {
                synchronized (mutexEventReceived)
                {
                    if(!eventReceived)
                    {
                        // Timeout occurred
                        eventsQueue.add(timeoutEvent);

                        if(mode == Mode.SINGLE)
                        {
                            // Stop the thread
                            keepGoing = false;
                        }
                    }

                    // Reset status
                    eventReceived = false;
                }
            }
        }
    };

    /**
     * Class constructor.
     *
     * @param eventsQueue The queue where the timeout event is eventually pushed.
     * @param timeoutMillis The timeout to be checked, in milliseconds.
     * @param timeoutEvent The event to send in case of timeout.
     */
    public TimeoutChecker(LinkedBlockingQueue<T> eventsQueue, int timeoutMillis, T timeoutEvent, Mode mode)
    {
        this.eventsQueue = eventsQueue;
        this.timeout = timeoutMillis;
        this.timeoutEvent = timeoutEvent;
        this.mode = mode;
    }

    /**
     * Start signal for the timeout checker.
     */
    public void start()
    {
        eventReceived = false;
        stopFlag = false;

        timeoutThread = new Thread(threadCode);
        timeoutThread.start();
    }

    /**
     * Stop signal for the timeout checker.
     */
    public void stop()
    {
        synchronized (mutexStopFlag)
        {
            stopFlag = true;
        }
    }

    /**
     * This function tells to the timeout checker that the expected event has
     * occurred. The timeout is then reset.
     */
    public void eventReceived()
    {
        synchronized (mutexEventReceived)
        {
            eventReceived = true;
        }
    }
}