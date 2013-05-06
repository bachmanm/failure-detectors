package broker;

/**
 * A broker with fixed delay of {@link #DELAY}.
 */
public class FixedDelayBroker extends ActiveMqBroker {

    /**
     * Construct a new broker.
     *
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public FixedDelayBroker(int numberOfProcesses) {
        super(numberOfProcesses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getDelay() {
        return DELAY;
    }
}
