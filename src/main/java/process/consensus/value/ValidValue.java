package process.consensus.value;

import message.ValueMessage;

/**
 * A valid {@link Value} encapsulating a value to be decided upon and the round number.
 * <p/>
 * This class is immutable, thus thread-safe.
 */
public class ValidValue implements Value {

    private final String value;
    private final int round;

    /**
     * Construct a new valid value.
     *
     * @param value encapsulated value.
     * @param round round number.
     */
    public ValidValue(String value, int round) {
        this.value = value;
        this.round = round;
    }

    /**
     * Get the encapsulated value.
     *
     * @return encapsulated value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the round number.
     *
     * @return round number.
     */
    public int getRound() {
        return round;
    }

    /**
     * Produce a message from this value, containing this value as the payload.
     *
     * @param source      source of the message.
     * @param destination destination of the message.
     * @return produced message.
     */
    public ValueMessage toMessage(int source, int destination) {
        return new ValueMessage(source, destination, getRound(), getValue());
    }

    /**
     * Produce a valid value from a received message.
     *
     * @param message to produce a value from..
     * @return produced value.
     */
    public static ValidValue fromMessage(ValueMessage message) {
        return new ValidValue(message.getValue(), message.getRound());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnknown() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuspect() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return true;
    }
}
