package process.base;

import com.sun.xml.internal.ws.util.CompletedFuture;
import detector.FailureDetector;
import org.apache.log4j.Logger;
import process.consensus.StrongConsensus;

import java.util.concurrent.*;

/**
 * A process capable of achieving consensus.
 */
public abstract class ConsensusCapableProcess<FD extends FailureDetector> extends FailureDetectorProcess<FD> {
    private static final Logger LOG = Logger.getLogger(ConsensusCapableProcess.class);

    protected final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected final String initialProposal;
    protected Future<String> decision;

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     * @param initialProposal   initial proposal for consensus.
     */
    public ConsensusCapableProcess(String name, int processId, int numberOfProcesses, String initialProposal) {
        super(name, processId, numberOfProcesses);
        this.initialProposal = initialProposal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void everybodyReady() {
        super.everybodyReady();

        if (initialProposal != null) {
            synchronized (this) {
                decision = startConsensus();
                notifyAll();
            }
        }
    }

    /**
     * Start the consensus reaching process.
     *
     * @return future consensus.
     */
    protected abstract Future<String> startConsensus();

    /**
     * Get the consensus result.
     *
     * @return result of the consensus as a future.
     */
    public Future<String> getDecision() {
        if (initialProposal == null) {
            return null;
        }

        synchronized (this) {
            while (decision == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted waiting for decision");
                }
            }
            return decision;
        }
    }
}
