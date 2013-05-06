package process;

import detector.StrongFailureDetector;
import process.base.ConsensusCapableProcess;
import process.consensus.StrongConsensus;

import java.util.concurrent.Future;

/**
 * A process with a {@link detector.StrongFailureDetector}, capable of achieving consensus.
 */
public class ConsensusCapableStrongFailureDetectorProcess extends ConsensusCapableProcess<StrongFailureDetector> {

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     * @param initialProposal   initial proposal for consensus.
     */
    public ConsensusCapableStrongFailureDetectorProcess(String name, int processId, int numberOfProcesses, String initialProposal) {
        super(name, processId, numberOfProcesses, initialProposal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StrongFailureDetector createFailureDetector() {
        return new StrongFailureDetector(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Future<String> startConsensus() {
        return executor.submit(StrongConsensus.createInstance(this, detector, initialProposal));
    }
}
