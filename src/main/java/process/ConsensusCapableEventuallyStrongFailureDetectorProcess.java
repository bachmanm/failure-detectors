package process;

import detector.EventuallyStrongFailureDetector;
import process.base.ConsensusCapableProcess;
import process.consensus.EventuallyStrongConsensus;

import java.util.concurrent.Future;

/**
 * A process with a {@link detector.EventuallyStrongFailureDetector}, capable of achieving consensus.
 */
public class ConsensusCapableEventuallyStrongFailureDetectorProcess extends ConsensusCapableProcess<EventuallyStrongFailureDetector> {

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     * @param initialProposal   initial proposal for consensus.
     */
    public ConsensusCapableEventuallyStrongFailureDetectorProcess(String name, int processId, int numberOfProcesses, String initialProposal) {
        super(name, processId, numberOfProcesses, initialProposal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected EventuallyStrongFailureDetector createFailureDetector() {
        return new EventuallyStrongFailureDetector(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Future<String> startConsensus() {
        return executor.submit(EventuallyStrongConsensus.createInstance(this, detector, initialProposal));
    }
}
