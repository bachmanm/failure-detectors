package detector;

import detector.timeout.AdaptiveAverageTimeoutStrategy;
import detector.timeout.AdaptiveMaxTimeoutStrategy;
import detector.timeout.TimeoutStrategy;
import message.TimestampedProcessToProcessMessage;

/**
 * Implementation of an eventually perfect failure detector.
 * <p/>
 * This failure detector initially suspects no processes. A process becomes a suspect when no message has been received
 * from it within a timeout period that gets updated with each received message (does not have to be heartbeat).
 * The strategy and default values for these timeouts are a responsibility of {@link detector.timeout.TimeoutStrategy}.
 * <p/>
 * A suspect process can become un-suspected when a message from that process arrives.
 * <p/>
 * This class is thread-safe.
 */
public class EventuallyPerfectFailureDetector extends StronglyCompleteFailureDetector {

    /**
     * Constructor.
     *
     * @param process to which this failure detector belongs.
     */
    public EventuallyPerfectFailureDetector(process.base.Process process) {
        super(process);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Schedules a new suspicion for the sender and removes the process from the list of suspects if present.
     */
    @Override
    public void doReceive(final TimestampedProcessToProcessMessage m) {
        scheduleNewSuspicion(m.getSource());
        removeFromSuspects(m.getSource());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TimeoutStrategy newTimeoutStrategy() {
        return new AdaptiveAverageTimeoutStrategy();
    }
}
