package detector.timeout;

import message.TimestampedMessage;

/**
 * A {@link TimeoutStrategy} which always returns the same value, which is
 * {@link TimeoutStrategy#DEFAULT_TIMEOUT_PERIOD}.
 * <p/>
 * This class has no state, intended to be used as singleton.
 */
public final class UpperBoundTimeoutStrategy implements TimeoutStrategy {

    private static final UpperBoundTimeoutStrategy INSTANCE = new UpperBoundTimeoutStrategy();

    public static UpperBoundTimeoutStrategy getInstance() {
        return INSTANCE;
    }

    private UpperBoundTimeoutStrategy() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(TimestampedMessage m) {
        //ignored, not needed as it is not adaptive
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNextTimeout() {
        return DEFAULT_TIMEOUT_PERIOD;
    }
}
