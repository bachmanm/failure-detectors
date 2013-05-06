package process;

import broker.Broker;
import broker.FixedDelayBroker;
import broker.GaussianDelayBroker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import process.base.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.*;
import static org.junit.Assert.assertEquals;

/**
 * Integration smoke-test for {@link process.LeaderElectingEventuallyPerfectFailureDetectorProcess}.
 * Gaussian delay is used.
 */
public class LeaderElectingEventuallyPerfectFailureDetectorProcessSmokeTest extends ProcessSmokeTest<LeaderElectingEventuallyPerfectFailureDetectorProcess> {

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
    protected LeaderElectingEventuallyPerfectFailureDetectorProcess createProcess(String processName, int processId, int numberOfProcesses) {
        return new LeaderElectingEventuallyPerfectFailureDetectorProcess(processName, processId, numberOfProcesses);
    }

    @Test
    public void assertLeadersChanges() throws InterruptedException, ExecutionException, IOException {
        LeaderElectingEventuallyPerfectFailureDetectorProcess p1 = processes.get(0);
        sleep(2000);

        assertEquals(10, p1.getLeader());

        killProcesses(9, 10);
        sleep(2000);

        assertEquals(8, p1.getLeader());

        killProcesses(3, 4);
        sleep(2000);

        assertEquals(8, p1.getLeader());

        killProcesses(2, 5, 6, 7, 8);
        sleep(2000);

        assertEquals(1, p1.getLeader());

        resurrectProcesses(4);
        sleep(2000);

        assertEquals(4, p1.getLeader());

        resurrectProcesses(2, 3, 5, 6, 7, 8, 9, 10);
        sleep(2000);

        assertEquals(10, p1.getLeader());
    }
}
