package detector;

import detector.timeout.TimeoutStrategy;
import detector.timeout.UpperBoundTimeoutStrategy;
import message.TimestampedProcessToProcessMessage;

/**
 * Implementation of a perfect failure detector.
 * <p/>
 * This failure detector initially suspects no processes. A process becomes a suspect when no message has not been
 * received from it within the last  {@link TimeoutStrategy#DEFAULT_TIMEOUT_PERIOD} (timeout period).
 * A suspect process is considered crashed and never recovers, all subsequent messages from this process are ignored.
 * <p/>
 * This class is thread-safe.
 */
public class PerfectFailureDetector extends StronglyCompleteFailureDetector {

    /**
     * Constructor.
     *
     * @param process to which this failure detector belongs.
     */
    public PerfectFailureDetector(process.base.Process process) {
        super(process);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Schedules a new timeout task for the sender,
     * unless the process is already suspect, in which case the message is ignored.
     */
    @Override
    public void doReceive(final TimestampedProcessToProcessMessage m) {
        if (!isSuspect(m.getSource())) {
            scheduleNewSuspicion(m.getSource());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TimeoutStrategy newTimeoutStrategy() {
        return UpperBoundTimeoutStrategy.getInstance();
    }
}
