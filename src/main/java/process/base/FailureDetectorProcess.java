package process.base;

import detector.FailureDetector;
import listener.MessageListener;

/**
 * An {@link Process} with a {@link FailureDetector} module.
 * <p/>
 * This class is thread-safe.
 */
public abstract class FailureDetectorProcess<FD extends FailureDetector> extends MessageHandlingProcess {

    protected final FD detector;

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public FailureDetectorProcess(String name, int processId, int numberOfProcesses) {
        super(name, processId, numberOfProcesses);
        detector = createFailureDetector();  //not ideal (the call leaks the reference to this before this is fully constructed
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void everybodyReady() {
        detector.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        super.stop();
        detector.stop();
    }

    /**
     * Create an instance of a failure detector for this process.
     *
     * @return new detector instance.
     */
    protected abstract FD createFailureDetector();
}
