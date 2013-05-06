package process.consensus.value;

/**
 * Placeholder for unknown values. Intended to be used as Singleton.
 */
public final class UnknownValue implements Value {

    private static final UnknownValue INSTANCE = new UnknownValue();

    /**
     * Private constructor to prevent instantiation.
     */
    private UnknownValue() {
    }

    /**
     * Get a singleton instance of this class.
     *
     * @return an instance.
     */
    public static UnknownValue getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnknown() {
        return true;
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
        return false;
    }
}
