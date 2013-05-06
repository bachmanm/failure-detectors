package process;

import detector.PerfectFailureDetector;
import process.base.FailureDetectorProcess;

/**
 * A process with a {@link detector.PerfectFailureDetector}.
 */
public class PerfectFailureDetectorProcess extends FailureDetectorProcess<PerfectFailureDetector> {

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public PerfectFailureDetectorProcess(String name, int processId, int numberOfProcesses) {
        super(name, processId, numberOfProcesses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PerfectFailureDetector createFailureDetector() {
        return new PerfectFailureDetector(this);
    }
}
