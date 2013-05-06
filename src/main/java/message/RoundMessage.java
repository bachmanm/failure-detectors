package message;

/**
 * A message containing a round number (for consensus-reaching algos).
 */
public abstract class RoundMessage extends TimestampedProcessToProcessMessage {

    private final int round;

    /**
     * Construct a new message.
     *
     * @param source      ID of the source process.
     * @param destination ID of the destination process.
     * @param round       round number when the value/outcome was reached.
     */
    public RoundMessage(int source, int destination, int round) {
        super(source, destination);
        this.round = round;
    }

    /**
     * Get the round number.
     *
     * @return round number.
     */
    public int getRound() {
        return round;
    }
}
