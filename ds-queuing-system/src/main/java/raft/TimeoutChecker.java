package raft;

import messages.Message;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is a wrapper around a thread, which is in charge of checking if a timeout is reached
 * before an event occurs.
 * If the event occurs the timeout is reset.
 * If the timeout triggers then a new event (the timeout event) is pushed in the event queue.
 */
public class TimeoutChecker
{
    /**
     * Defines the behaviour of the timeout checker once a timeout
     * event is reached.
     */
    public enum Mode
    {
        /** Stops when a timeout is reached or eventReceived() is called or
         * stop() is called.*/
        // Removed because I've removed eventReceived()
//        SINGLE,

        /** Stop the timeout procedure once a timeout event is reached or
         * stop() is called. */
        EXPLICIT,

        /** Keep the procedure going. It might generate multiple events. */
//        MULTIPLE,
    }

    /**
     * The queue where events are published, needed to publish the eventual
     * timeout event.
     */
    private LinkedBlockingQueue<Message> eventsQueue;

    /**
     * The event to be sent in the queue in case of timeout.
     */
    private Message timeoutEvent;

    /**
     * Timeout value in milliseconds.
     */
    private int timeout;

    /**
     * Period of time between one sleep of the timeout thread and the other.
     * Expressed in milliseconds.
     *
     * This is done so that there's no need to wait for the entire timeout
     * before we can safely stop the thread and join: if the timeout is 5
     * seconds and after 1s we want to stop, we have to wait for other 4
     * seconds before the join succeeds.
     */
    private static final Integer sleepPeriod = 50;

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
     * This variable indicates whether the thread is currently
     * running or not.
     */
    private boolean running = false;
    private final Object mutexRunning = new Object();

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

        synchronized (mutexRunning)
        {
            running = true;
        }

        while(keepGoing)
        {
            // Sleep period
            int i = 0;
            while(keepGoing && sleepPeriod * i < timeout)
            {
                try
                {
                    Thread.sleep(sleepPeriod);
                } catch (Exception e) {}
                synchronized (mutexStopFlag)
                {
                    keepGoing = !stopFlag;
                }

                i++;
            }

            if(keepGoing)
            {
                synchronized (mutexEventReceived)
                {
                    if(!eventReceived)
                    {
                        // Timeout occurred
                        eventsQueue.add(timeoutEvent);

                        // Commented, multiple is not supported, so it should always stop
//                        if(mode != Mode.MULTIPLE)
//                        {
//                            // Stop the thread
//                            keepGoing = false;
//                        }

                        // Stop the thread
                        keepGoing = false;
                    }

                    // Reset status
                    eventReceived = false;
                }
            }
        }

        synchronized (mutexRunning)
        {
            running = false;
        }
    };

    /**
     * Class constructor.
     *
     * @param eventsQueue The queue where the timeout event is eventually pushed.
     */
    public TimeoutChecker(LinkedBlockingQueue<Message> eventsQueue)
    {
        this.eventsQueue = eventsQueue;

        this.timeout = 0;
        this.timeoutEvent = null;
        this.mode = null;
    }

    /**
     * Start signal for the timeout checker. If already running
     * the timeout is stopped and a new one is started.
     *
     * @param timeoutMillis The timeout to be checked, in milliseconds.
     * @param timeoutEvent The event to send in case of timeout.
     * @param mode TODO
     */
    public void startNewTimeout(final int timeoutMillis, final Message timeoutEvent, final Mode mode)
    {
        if(isRunning())
        {
            System.out.println("TimeoutChecker::startNewTimeout(): already going, stopping now and restarting");
            disableAndRemove();
        }

        this.timeout = timeoutMillis;
        this.timeoutEvent = timeoutEvent;
        this.mode = mode;

        // No need to sync with the mutex, as the thread
        // shouldn't be running
        eventReceived = false;
        stopFlag = false;
        running = false;

        timeoutThread = new Thread(threadCode);
        timeoutThread.start();
    }

    /**
     * This function is used to disable the timeout checker and remove instances
     * of the generated event in case the timeout has already fired but the event
     * hasn't been already processed.
     * This covers the case in which I'm waiting for an event, this event occurs
     * but the timeout fires before I'm able to stop it. In this case I want to
     * remove the event fired from the event queue.
     */
    public void disableAndRemove()
    {
        if(!isRunning())
        {
            return;
        }

        stop();

        removeFiredEvent();
    }

    /**
     * @return True if the timeout is currently running. False if it hasn't
     * started yet or if it has stopped.
     */
    public boolean isRunning()
    {
        synchronized (mutexRunning)
        {
            return running;
        }
    }

    /**
     * Utility to correctly stop the timeout checker.
     */
    private void stop()
    {
        synchronized (mutexStopFlag)
        {
            stopFlag = true;
        }

        try
        {
            timeoutThread.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Utility to remove all the instances of the event (to be fired from
     * this timeout) in the events queue.
     */
    private void removeFiredEvent()
    {
        // Removing all occurrences of the event
        Iterator<Message> iterator = eventsQueue.iterator();
        while (iterator.hasNext()) {
            Message msg = iterator.next();

            if (timeoutEvent.type.equals(msg.type)) {
                iterator.remove();
            }
        }
    }

    /**
     * This function tells to the timeout checker that the expected event has
     * occurred. The timeout is then reset.
     */
//    public void eventReceived()
//    {
//        synchronized (mutexEventReceived)
//        {
//            eventReceived = true;
//        }
//
//        if(mode == Mode.SINGLE)
//        {
//            stop();
//        }
//    }
}