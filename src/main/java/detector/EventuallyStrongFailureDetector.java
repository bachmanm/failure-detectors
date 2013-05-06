package detector;

/**
 * Eventually strong failure detector.
 * <p/>
 * It extends {@link EventuallyPerfectFailureDetector} because eventually perfect implies eventually strong.
 * <p/>
 * This class is thread-safe.
 */
public class EventuallyStrongFailureDetector extends EventuallyPerfectFailureDetector {

    /**
     * Constructor.
     *
     * @param process to which this failure detector belongs.
     */
    public EventuallyStrongFailureDetector(process.base.Process process) {
        super(process);
    }
}
