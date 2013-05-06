package process.consensus.value;

import message.OutcomeMessage;

/**
 * A {@link ValidValue} encapsulating an outcome of a decision, i.e. the value, the round number, and whether
 * the decision is unanimous.
 * <p/>
 * This class is immutable, thus thread-safe.
 */
public class Outcome extends ValidValue {

    private final boolean unanimous;

    /**
     * Construct a new outcome.
     *
     * @param unanimous whether the outcome was an unanimous decision.
     * @param value     encapsulated value.
     * @param round     round number.
     */
    public Outcome(boolean unanimous, String value, int round) {
        super(value, round);
        this.unanimous = unanimous;
    }

    /**
     * Was the outcome an unanimous decision?
     *
     * @return true if and only if the outcome was unanimous.
     */
    public boolean isUnanimous() {
        return unanimous;
    }

    /**
     * Produce a message from this outcome, containing this outcome as the payload.
     *
     * @param source      source of the message.
     * @param destination destination of the message.
     * @return produced message.
     */
    public OutcomeMessage toMessage(int source, int destination) {
        return new OutcomeMessage(source, destination, getRound(), getValue(), isUnanimous());
    }

    /**
     * Produce an outcome from a received message.
     *
     * @param message to produce an outcome from..
     * @return produced outcome.
     */
    public static Outcome fromMessage(OutcomeMessage message) {
        return new Outcome(message.isUnanimous(), message.getValue(), message.getRound());
    }
}
