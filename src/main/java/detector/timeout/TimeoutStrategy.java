package detector.timeout;

import broker.ActiveMqBroker;
import detector.FailureDetector;
import detector.StronglyCompleteFailureDetector;
import message.TimestampedMessage;

import static detector.StronglyCompleteFailureDetector.*;

/**
 * A strategy for finding the next timeout period for a process.
 */
public interface TimeoutStrategy {

    /**
     * Default timeout for adaptive strategies and THE timeout for fixed-timeout strategies.
     */
    public static final int DEFAULT_TIMEOUT_PERIOD = HEARTBEAT_PERIOD_MS + 2 * ActiveMqBroker.DELAY;

    /**
     * Acknowledge a received message from process.
     *
     * @param m the message.
     */
    void messageReceived(TimestampedMessage m);

    /**
     * Compute the next timeout to be used for a process.
     *
     * @return the next timeout in ms.
     */
    long getNextTimeout();

}
