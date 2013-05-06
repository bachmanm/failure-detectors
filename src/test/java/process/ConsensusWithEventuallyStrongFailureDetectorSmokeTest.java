package process;

import broker.Broker;
import broker.FixedDelayBroker;
import broker.GaussianDelayBroker;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Integration smoke-test for {@link process.ConsensusCapableEventuallyStrongFailureDetectorProcess}.
 * Gaussian delay is used.
 */
public class ConsensusWithEventuallyStrongFailureDetectorSmokeTest extends ProcessSmokeTest<ConsensusCapableEventuallyStrongFailureDetectorProcess> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() {
        super.setUp();
        launchProcesses(10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Broker createBroker() {
        return new GaussianDelayBroker(10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConsensusCapableEventuallyStrongFailureDetectorProcess createProcess(String processName, int processId, int numberOfProcesses) {
        return new ConsensusCapableEventuallyStrongFailureDetectorProcess(processName, processId, numberOfProcesses, "value" + processId);
    }

    @Test
    public void whenNoProcessesFailTheFirstProcessValueIsDecided() throws InterruptedException, ExecutionException, IOException {
        ConsensusCapableEventuallyStrongFailureDetectorProcess p1 = processes.get(0);

        Future<String> consensusResult = p1.getDecision();

        assertTrue(consensusResult.get().startsWith("value"));
    }

    @Test
    public void whenFirstProcessFailsAValueIsDecided() throws InterruptedException, ExecutionException, IOException {
        ConsensusCapableEventuallyStrongFailureDetectorProcess p5 = processes.get(4);

        Future<String> consensusResult = p5.getDecision();

        killProcesses(1);

        assertNotSame("value1", consensusResult.get());
    }

    @Test
    public void whenThreeProcessesFailValueIsDecided() throws InterruptedException, ExecutionException, IOException {
        ConsensusCapableEventuallyStrongFailureDetectorProcess p5 = processes.get(4);

        Future<String> consensusResult = p5.getDecision();

        killProcesses(1, 2, 3);

        assertNotSame("value1", consensusResult.get());
        assertNotSame("value2", consensusResult.get());
        assertNotSame("value3", consensusResult.get());
    }
}
