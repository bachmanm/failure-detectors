package detector.timeout;

import detector.FailureDetector;
import detector.StronglyCompleteFailureDetector;
import message.TimestampedMessage;

import static broker.ActiveMqBroker.DELAY;
import static detector.StronglyCompleteFailureDetector.*;

/**
 * An adaptive {@link TimeoutStrategy} based on a process' communication history.
 * Computes the next timeout as {@link StronglyCompleteFailureDetector#HEARTBEAT_PERIOD_MS} + the maximum delay of the process'
 * messages seen thus far.
 * <p/>
 * This class is thread-safe, all non-private access is synchronized.
 */
public final class AdaptiveMaxTimeoutStrategy implements TimeoutStrategy {

    private long maxDelay = DELAY;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void messageReceived(TimestampedMessage m) {
        maxDelay = Math.max(maxDelay, m.getDelay());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long getNextTimeout() {
        return HEARTBEAT_PERIOD_MS + maxDelay;
    }
}
