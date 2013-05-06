package message;

/**
 * A message indicating a decision outcome for consensus-reaching algorithms.
 */
public class OutcomeMessage extends ValueMessage {

    private final boolean unanimous;

    /**
     * Construct a new message.
     *
     * @param source      ID of the source process.
     * @param destination ID of the destination process.
     * @param round       round number when the outcome was reached.
     * @param value       outcome value.
     * @param unanimous   whether the outcome is unanimous.
     */
    public OutcomeMessage(int source, int destination, int round, String value, boolean unanimous) {
        super(source, destination, round, value);
        this.unanimous = unanimous;
    }

    /**
     * Is the outcome unanimous?
     *
     * @return true iff the outcome is unanimous.
     */
    public boolean isUnanimous() {
        return unanimous;
    }

    @Override
    public String toString() {
        return getValue()+" from " + getSource() + " in round " + getRound() + ", unanimous:" + isUnanimous();
    }
}
