package detector;

/**
 * Strong failure detector.
 * <p/>
 * It extends {@link PerfectFailureDetector} because perfect implies strong.
 * <p/>
 * This class is thread-safe.
 */
public class StrongFailureDetector extends PerfectFailureDetector {

    /**
     * Constructor.
     *
     * @param process to which this failure detector belongs.
     */
    public StrongFailureDetector(process.base.Process process) {
        super(process);
    }
}
