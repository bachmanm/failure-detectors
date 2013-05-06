package broker;

import java.util.Random;

/**
 * A broker with Gaussian delay with {@link GaussianDelayBroker#MEAN} and {@link GaussianDelayBroker#STDV}.
 */
public class GaussianDelayBroker extends ActiveMqBroker {
    private static final double MEAN = (double) DELAY;
    private static final double STDV = MEAN / 2.0;

    private final Random random = new Random(System.currentTimeMillis());

    /**
     * Construct a new broker.
     *
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public GaussianDelayBroker(int numberOfProcesses) {
        super(numberOfProcesses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getDelay() {
        int result = (int) Math.round((random.nextGaussian() * STDV) + MEAN);
        if (result < 0) {
            return 0;
        }
        return result;
    }
}
