package message;

/**
 * A message with a creation timestamp.
 */
public interface TimestampedMessage {

    /**
     * Get the timestamp of the message.
     *
     * @return message timestamp in ms since 1/1/1970.
     */
    long getTimestamp();

    /**
     * Get the delay of this message, i.e. the time that has elapsed between when the message was created and now.
     *
     * @return delay in ms.
     */
    long getDelay();
}
