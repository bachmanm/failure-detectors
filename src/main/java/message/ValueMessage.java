package message;

/**
 * A message indicating a decision value for consensus-reaching algorithms.
 */
public class ValueMessage extends RoundMessage {

    private final String value;

    /**
     * Construct a new message.
     *
     * @param source      ID of the source process.
     * @param destination ID of the destination process.
     * @param round       round number when the value was decided.
     * @param value       decided value.
     */
    public ValueMessage(int source, int destination, int round, String value) {
        super(source, destination, round);
        this.value = value;
    }

    /**
     * Ge the decided value.
     *
     * @return value.
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue()+" from " + getSource() + " in round " + getRound();
    }

}
