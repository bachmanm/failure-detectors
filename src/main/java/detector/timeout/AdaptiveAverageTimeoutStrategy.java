package detector.timeout;

import detector.FailureDetector;
import detector.StronglyCompleteFailureDetector;
import message.TimestampedMessage;

import static broker.ActiveMqBroker.DELAY;
import static detector.StronglyCompleteFailureDetector.*;

/**
 * An adaptive {@link TimeoutStrategy} based on a process' communication history.
 * Computes the next timeout as {@link detector.StronglyCompleteFailureDetector#HEARTBEAT_PERIOD_MS} + 2 * the average delay of the process'
 * messages seen thus far.
 * <p/>
 * This class is thread-safe, all non-private access is synchronized.
 */
public final class AdaptiveAverageTimeoutStrategy implements TimeoutStrategy {

    private long totalDelay = DELAY;
    private int totalMessages = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void messageReceived(TimestampedMessage m) {
        totalDelay += m.getDelay();
        totalMessages++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long getNextTimeout() {
        return Math.round(HEARTBEAT_PERIOD_MS + 2 * (totalDelay * 1.0 / totalMessages));
    }
}
