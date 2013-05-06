package process.consensus.value;

/**
 * An internal representation of a value passed around the processes for the purposes of achieving some common goal,
 * such as electing a leader or reaching a consensus.
 */
public interface Value {

    /**
     * Does this instance represent an unknown value?
     *
     * @return true if an only if this represents an unknown value.
     */
    boolean isUnknown();

    /**
     * Does this instance represent a placeholder for a value that wasn't received from a process because it is suspect?
     *
     * @return true if and only if this represents a placeholder for a value not received because a process might have failed.
     */
    boolean isSuspect();

    /**
     * Does this instance represent a valid value, i.e. value received from a correct process?
     *
     * @return true if and only is this represents a value received from a correct process. If this method returns true,
     *         one of {@link Value#isSuspect()} or {@link Value#isUnknown} must return false.
     */
    boolean isValid();
}
