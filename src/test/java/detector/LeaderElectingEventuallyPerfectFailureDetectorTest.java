package detector;

import message.HeartbeatMessage;
import org.junit.Before;
import org.junit.Test;
import process.base.Process;

import static detector.timeout.TimeoutStrategy.DEFAULT_TIMEOUT_PERIOD;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link detector.LeaderElectingEventuallyPerfectFailureDetector}.
 */
public class LeaderElectingEventuallyPerfectFailureDetectorTest {

    private LeaderElectingEventuallyPerfectFailureDetector detector;

    @Before
    public void createDetector() {
        Process mockProcess = mock(Process.class);

        detector = new LeaderElectingEventuallyPerfectFailureDetector(mockProcess);
        when(mockProcess.getNumberOfProcesses()).thenReturn(3);
        when(mockProcess.getProcessId()).thenReturn(1);
        detector.start();
    }

    @Test
    public void firstLeaderShouldBeElectedImmediatelyAfterBegin() {
        assertEquals(3, detector.getLeader());
    }

    @Test
    public void whenLeaderDiesNewOneShouldBeElected() throws InterruptedException {
        sleep(DEFAULT_TIMEOUT_PERIOD / 2 + 10);

        detector.receive(new HeartbeatMessage(2));

        sleep(DEFAULT_TIMEOUT_PERIOD / 2 + 10);

        assertEquals(2, detector.getLeader());
    }

    @Test
    public void whenAllProcessesDieElectItselfAsLeader() throws InterruptedException { //(processes never suspect themselves)
        sleep(DEFAULT_TIMEOUT_PERIOD + 10);

        assertEquals(1, detector.getLeader());
    }

    @Test
    public void whenProcessesBecomeUnsuspectTheyCanBecomeLeaders() throws InterruptedException {
        sleep(DEFAULT_TIMEOUT_PERIOD / 2 + 10);

        detector.receive(new HeartbeatMessage(2));

        sleep(DEFAULT_TIMEOUT_PERIOD / 2 + 10);

        assertEquals(2, detector.getLeader());

        detector.receive(new HeartbeatMessage(2));
        detector.receive(new HeartbeatMessage(3));

        sleep(DEFAULT_TIMEOUT_PERIOD / 2 + 10);

        assertEquals(3, detector.getLeader());
    }
}
