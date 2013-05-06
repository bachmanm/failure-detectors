package process.consensus.value;

/**
 * Placeholder for values not received because the sending process might have crashed. Intended to be used as Singleton.
 */
public final class SuspectValue implements Value {

    private static final SuspectValue INSTANCE = new SuspectValue();

    /**
     * Private constructor to prevent instantiation.
     */
    private SuspectValue() {
    }

    /**
     * Get a singleton instance of this class.
     *
     * @return an instance.
     */
    public static SuspectValue getInstance() {
        return INSTANCE;
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
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return false;
    }
}
