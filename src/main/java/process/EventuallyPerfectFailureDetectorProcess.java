package process;

import detector.EventuallyPerfectFailureDetector;
import process.base.FailureDetectorProcess;

/**
 * A process with a {@link detector.EventuallyPerfectFailureDetector}.
 */
public class EventuallyPerfectFailureDetectorProcess extends FailureDetectorProcess<EventuallyPerfectFailureDetector> {

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public EventuallyPerfectFailureDetectorProcess(String name, int processId, int numberOfProcesses) {
        super(name, processId, numberOfProcesses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EventuallyPerfectFailureDetector createFailureDetector() {
        return new EventuallyPerfectFailureDetector(this);
    }
}
