package process.demo;

import broker.Broker;
import broker.FixedDelayBroker;
import broker.GaussianDelayBroker;
import org.junit.Ignore;
import org.junit.Test;
import process.EventuallyPerfectFailureDetectorProcess;
import process.ProcessSmokeTest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

/**
 * Demo of {@link process.EventuallyPerfectFailureDetectorProcess}. Gaussian delay is used.
 */
@Ignore("only for demo purposes")
public class EventuallyPerfectFailureDetectorDemo extends ProcessSmokeTest<EventuallyPerfectFailureDetectorProcess> {

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
    protected EventuallyPerfectFailureDetectorProcess createProcess(String processName, int processId, int numberOfProcesses) {
        return new EventuallyPerfectFailureDetectorProcess(processName, processId, numberOfProcesses);
    }

    //set the debug level to TRACE to see heartbeats
    @Test
    public void justWatchNoFailures() throws InterruptedException, ExecutionException, IOException {
        sleep(100000);
    }

    @Test
    public void justWatchWithFailures() throws InterruptedException, ExecutionException, IOException {
        sleep(1000);

        killProcesses(9, 10);
        sleep(2000);

        killProcesses(3, 4);
        sleep(2000);

        killProcesses(1, 2, 5, 6, 7, 8);
        sleep(2000);

        resurrectProcesses(4);
        sleep(2000);

        resurrectProcesses(1, 2, 3, 5, 6, 7, 8, 9, 10);
        sleep(100000);
    }
}
