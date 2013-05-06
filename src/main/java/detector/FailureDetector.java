package detector;

import listener.SuspectListener;
import message.TimestampedProcessToProcessMessage;

import java.util.Set;

/**
 * A failure detector module that can be used by a process to overcome the impossibility result of reaching consensus
 * in an asynchronous system in the presence of failures.
 * <p/>
 * It maintains a list of processes that are suspected of having failed and provides that information to the owning process.
 */
public interface FailureDetector {

    /**
     * Begin the operation of the detector.
     */
    void start();

    /**
     * Stop operation.
     */
    void stop();

    /**
     * Is a process suspected of having failed?
     *
     * @param process ID of a process to check.
     * @return true iff the process is suspected.
     */
    boolean isSuspect(int process);

    /**
     * Get a read-only copy of the current suspect set.
     *
     * @return read-only copy of the current suspect set.
     */
    Set<Integer> getSuspects();

    /**
     * Register a listener to be notified of suspect changes.
     *
     * @param suspectListener to register.
     */
    void addSuspectListener(SuspectListener suspectListener);
}
