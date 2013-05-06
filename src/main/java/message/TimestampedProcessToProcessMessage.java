package message;

/**
 * A {@link TimestampedMessage} {@link ProcessToProcessMessage}.
 */
public class TimestampedProcessToProcessMessage implements ProcessToProcessMessage, TimestampedMessage {

    private final int source;
    private final int destination;
    private final long timestamp;

    /**
     * Construct a new message with timestamp = now.
     *
     * @param source      ID of the source process.
     * @param destination ID of the destination process.
     */
    public TimestampedProcessToProcessMessage(int source, int destination) {
        this.source = source;
        this.destination = destination;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSource() {
        return source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDestination() {
        return destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDelay() {
        return System.currentTimeMillis() - getTimestamp();
    }
}
