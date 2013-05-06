package process;

import broker.Broker;
import broker.FixedDelayBroker;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * Integration smoke-test for {@link process.ConsensusCapableStrongFailureDetectorProcess}. Fixed delay is used.
 */
public class ConsensusWithStrongFailureDetectorSmokeTest extends ProcessSmokeTest<ConsensusCapableStrongFailureDetectorProcess> {

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
        return new FixedDelayBroker(10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConsensusCapableStrongFailureDetectorProcess createProcess(String processName, int processId, int numberOfProcesses) {
        return new ConsensusCapableStrongFailureDetectorProcess(processName, processId, numberOfProcesses, "value" + processId);
    }

    @Test
    public void whenNoProcessesFailTheFirstProcessValueIsDecided() throws InterruptedException, ExecutionException, IOException {
        ConsensusCapableStrongFailureDetectorProcess p1 = processes.get(0);

        Future<String> consensusResult = p1.getDecision();

        assertEquals("value1", consensusResult.get());
    }

    @Test
    public void whenFirstProcessesFailTheSecondProcessValueIsDecided() throws InterruptedException, ExecutionException, IOException {
        ConsensusCapableStrongFailureDetectorProcess p5 = processes.get(4);

        killProcesses(1);

        Future<String> consensusResult = p5.getDecision();

        assertEquals("value2", consensusResult.get());
    }

    @Test
    public void whenManyProcessesFailValueIsDecided() throws InterruptedException, ExecutionException, IOException {
        ConsensusCapableStrongFailureDetectorProcess p5 = processes.get(4);

        killProcesses(1);
        killProcesses(2);
        killProcesses(3);
        killProcesses(4);
        killProcesses(6);
        killProcesses(7);
        killProcesses(8);

        Future<String> consensusResult = p5.getDecision();

        assertEquals("value5", consensusResult.get());
    }
}
