package process;

import detector.LeaderElectingEventuallyPerfectFailureDetector;
import process.base.FailureDetectorProcess;

/**
 * A process with a {@link detector.LeaderElectingEventuallyPerfectFailureDetector}.
 */
public class LeaderElectingEventuallyPerfectFailureDetectorProcess extends FailureDetectorProcess<LeaderElectingEventuallyPerfectFailureDetector> {

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public LeaderElectingEventuallyPerfectFailureDetectorProcess(String name, int processId, int numberOfProcesses) {
        super(name, processId, numberOfProcesses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LeaderElectingEventuallyPerfectFailureDetector createFailureDetector() {
        return new LeaderElectingEventuallyPerfectFailureDetector(this);
    }

    /**
     * Get the current leader of the system.
     *
     * @return process ID of the current leader.
     */
    public int getLeader() {
        return detector.getLeader();
    }
}
