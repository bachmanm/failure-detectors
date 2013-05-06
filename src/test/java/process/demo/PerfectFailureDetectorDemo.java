package process.demo;

import broker.Broker;
import broker.FixedDelayBroker;
import detector.PerfectFailureDetector;
import org.junit.Ignore;
import org.junit.Test;
import process.PerfectFailureDetectorProcess;
import process.ProcessSmokeTest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;

/**
 * Demo of {@link process.PerfectFailureDetectorProcess}. Fixed delay is used.
 */
@Ignore("only for demo purposes")
public class PerfectFailureDetectorDemo extends ProcessSmokeTest<PerfectFailureDetectorProcess> {

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
    protected PerfectFailureDetectorProcess createProcess(String processName, int processId, int numberOfProcesses) {
        return new PerfectFailureDetectorProcess(processName, processId, numberOfProcesses);
    }

    /**
     * No processes are considered correct again after failing once.
     */
    @Test
    public void justWatchAllDieOff() throws InterruptedException, ExecutionException, IOException {
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
        sleep(10000);
    }
}
