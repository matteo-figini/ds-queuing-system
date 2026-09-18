package raft;

import java.io.*;
import java.util.List;

/**
 * Class used to handle the log files operations (saving and loading to/from disk).
 * @param <T> The log item that will be saved to disk.
 */
public class LogFilesHandler<T> {

    /**
     * Struct used to retrieve data from the log file, because java sucks.
     */
    public class StatusStructure
    {
        public final Integer currentTerm;
        public final String votedFor;
        public final Integer commitLength;
        public StatusStructure(Integer currentTerm, String votedFor, Integer commitLength)
        {
            this.currentTerm = currentTerm;
            this.votedFor = votedFor;
            this.commitLength = commitLength;
        }
    }

    /** The path of the file that saves the node's log. */
    private final String logFilePath;

    /**
     * The path of the file that saves the node's status variables: currentTerm,
     * votedFor and commitLength.
     */
    private final String statusFilePath;

    public LogFilesHandler(String logFilePath, String statusFilePath)
    {
        this.logFilePath = logFilePath;
        this.statusFilePath = statusFilePath;
    }

    /**
     * Check if both log files to recover from.
     *
     * @return True if the files exists, false otherwise.
     */
    public boolean logExists()
    {
        File logFile = new File(logFilePath);
        if(!logFile.isFile())
        {
            return false;
        }

        File statusFile = new File(statusFilePath);
        return statusFile.isFile();
    }

    /**
     * Save the status variables on disk. The order in which are saved is:
     * currentTerm, votedFor, commitLength.
     *
     * @return True if operation successful, false otherwise.
     */
    public boolean saveStatus(final Integer currentTerm, final String votedFor, final Integer commitLength)
    {
        // TODO: error handling
        try
        {
            FileOutputStream fos = new FileOutputStream(statusFilePath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(currentTerm);
            oos.writeObject(votedFor);
            oos.writeObject(commitLength);

            oos.close();
        }
        catch(Exception e)
        {
            return false;
        }

        return true;
    }

    /**
     * Utility for saving the log on disk. First it saves the log length, then the actual log items.
     *
     * @return True if operation successful, false otherwise.
     */
    public boolean saveLog(final List<T> log)
    {
        // TODO: error handling
        try
        {
            FileOutputStream fos = new FileOutputStream(logFilePath);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            final Integer SIZE = log.size();
            oos.writeObject(SIZE);

            for(T item : log)
            {
                oos.writeObject(item);
            }

            oos.close();
        }
        catch(Exception e)
        {
            return false;
        }

        return true;
    }

    /**
     * Load the status variables from disk.
     *
     * @return The structure containing the variables.
     */
    public StatusStructure loadStatus() throws IOException
    {
        Integer currentTerm = 0;
        String votedFor = "";
        Integer commitLength = 0;

        if(!logExists())
        {
            // TODO: exception? Improve?
            throw new IOException("Error, cannot find backup files");
        }

        try
        {
            FileInputStream fis = new FileInputStream(statusFilePath);
            ObjectInputStream ois = new ObjectInputStream(fis);

            currentTerm = (Integer) ois.readObject();
            votedFor = (String) ois.readObject();
            commitLength = (Integer) ois.readObject();

            ois.close();
        }
        catch (Exception e)
        {
            // TODO: exception? Improve?
            throw new IOException("Failed to load status from disk");
        }

        return new StatusStructure(currentTerm, votedFor, commitLength);
    }

    /**
     * Load the log from disk.
     * @param log The list that will be filled.
     */
    public void loadLog(List<T> log) throws IOException
    {
        if(!logExists())
        {
            // TODO: exception? Improve?
            throw new IOException("Error, cannot find backup files");
        }

        log.clear();

        try
        {
            FileInputStream fis = new FileInputStream(logFilePath);
            ObjectInputStream ois = new ObjectInputStream(fis);

            final Integer SIZE = (Integer) ois.readObject();

            for(int i = 0; i < SIZE; ++i)
            {
                T item = (T) ois.readObject();
                log.add(item);
            }

            ois.close();
        }
        catch (Exception e)
        {
            // TODO: exception? Improve?
            throw new IOException("Failed to load log from disk");
        }
    }
}
