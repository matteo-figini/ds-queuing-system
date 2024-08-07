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
     * This flag signals when the timeout was disabled by the user,
     * regardless of the timeout status (triggered, event received,
     * still running). If the timeout was disabled then the event
     * that it might have generated must be ignored.
     */
    private boolean disabled = false;

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
     * @param timeoutMillis The timeout to be checked, in milliseconds.
     * @param timeoutEvent The event to send in case of timeout.
     */
    public TimeoutChecker(LinkedBlockingQueue<Message> eventsQueue, int timeoutMillis, Message timeoutEvent, Mode mode)
    {
        this.eventsQueue = eventsQueue;
        this.timeout = timeoutMillis;
        this.timeoutEvent = timeoutEvent;
        this.mode = mode;
    }

    /**
     * Start signal for the timeout checker.
     */
    public void startNewTimeout()
    {
        // TODO: remove this assert and substitute it with a check that forces the timeout
        //  to safely stop. Then create a new one.
        if(isRunning())
        {
//            throw new RuntimeException("TimeoutChecker::startNewTimeout(): ALREADY STARTED");
            System.out.println("TimeoutChecker::startNewTimeout(): already going, stopping now and restarting");
            disableAndRemove();
        }

        disabled = false;
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
        disabled = true;

        stop();

        removeFiredEvent();
    }

    public boolean isDisabled()
    {
        return disabled;
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