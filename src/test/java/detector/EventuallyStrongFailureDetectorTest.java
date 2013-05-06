package detector;

import listener.SuspectListener;
import message.HeartbeatMessage;
import org.junit.Before;
import org.junit.Test;
import process.base.Process;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static detector.StronglyCompleteFailureDetector.HEARTBEAT_PERIOD_MS;
import static detector.timeout.TimeoutStrategy.DEFAULT_TIMEOUT_PERIOD;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventuallyStrongFailureDetector}.
 */
public class EventuallyStrongFailureDetectorTest {

    private Process mockProcess;
    private EventuallyStrongFailureDetector detector;

    @Before
    public void createDetector() {
        mockProcess = mock(Process.class);

        detector = new EventuallyStrongFailureDetector(mockProcess);
    }

    @Test
    public void noHeartbeatShouldBeSentWithoutBegin() {
        verifyNoMoreInteractions(mockProcess);
    }

    @Test
    public void oneHeartbeatShouldBeSentAfterBegin() throws InterruptedException {
        detector.start();
        sleep(10);

        verify(mockProcess).send(any(HeartbeatMessage.class));
        verify(mockProcess).getNumberOfProcesses();
        verify(mockProcess).addMessageListener(detector);
        verify(mockProcess).getProcessId();
        verifyNoMoreInteractions(mockProcess);
    }

    @Test
    public void twoHeartbeatsShouldBeSentAfterOnePeriod() throws InterruptedException {
        detector.start();
        sleep(HEARTBEAT_PERIOD_MS + 10);

        verify(mockProcess, times(2)).send(any(HeartbeatMessage.class));
        verify(mockProcess, times(2)).getProcessId();
        verify(mockProcess).getNumberOfProcesses();
        verify(mockProcess).addMessageListener(detector);
        verifyNoMoreInteractions(mockProcess);
    }

    @Test
    public void threeHeartbeatsShouldBeSentAfterTwoPeriods() throws InterruptedException {
        detector.start();
        sleep(2 * HEARTBEAT_PERIOD_MS + 10);

        verify(mockProcess, times(3)).send(any(HeartbeatMessage.class));
        verify(mockProcess, times(3)).getProcessId();
        verify(mockProcess).getNumberOfProcesses();
        verify(mockProcess).addMessageListener(detector);
        verifyNoMoreInteractions(mockProcess);
    }

    @Test
    public void noProcessesShouldInitiallyBeSuspected() {
        when(mockProcess.getNumberOfProcesses()).thenReturn(3);
        when(mockProcess.getProcessId()).thenReturn(1);
        detector.start();

        assertFalse(detector.isSuspect(0));
        assertFalse(detector.isSuspect(1));
        assertFalse(detector.isSuspect(2));
        assertFalse(detector.isSuspect(3));
    }

    @Test
    public void noProcessesShouldBeSuspectedBeforeEndOfFirstPeriod() throws InterruptedException {
        when(mockProcess.getNumberOfProcesses()).thenReturn(3);
        when(mockProcess.getProcessId()).thenReturn(1);
        detector.start();
        sleep(HEARTBEAT_PERIOD_MS - 10);

        assertFalse(detector.isSuspect(0));
        assertFalse(detector.isSuspect(1));
        assertFalse(detector.isSuspect(2));
        assertFalse(detector.isSuspect(3));
    }

    @Test
    public void allOtherProcessesShouldBeSuspectedWhenNoHeartBeatHasBeenReceived() throws InterruptedException {
        when(mockProcess.getNumberOfProcesses()).thenReturn(3);
        when(mockProcess.getProcessId()).thenReturn(1);
        detector.start();
        sleep(DEFAULT_TIMEOUT_PERIOD + 10);

        assertFalse(detector.isSuspect(0)); //Registrar should never be suspected
        assertFalse(detector.isSuspect(1)); //This process should not suspect itself
        assertTrue(detector.isSuspect(2));
        assertTrue(detector.isSuspect(3));
    }

    @Test
    public void processSendingHeartbeatShouldNotBeSuspected() throws InterruptedException {
        when(mockProcess.getNumberOfProcesses()).thenReturn(3);
        when(mockProcess.getProcessId()).thenReturn(1);
        detector.start();

        sleep(DEFAULT_TIMEOUT_PERIOD /2 + 10);

        detector.receive(new HeartbeatMessage(2));

        sleep(DEFAULT_TIMEOUT_PERIOD /2 + 100);

        assertFalse(detector.isSuspect(0)); //Registrar should never be suspected
        assertFalse(detector.isSuspect(1)); //This process should not suspect itself
        assertFalse(detector.isSuspect(2));
        assertTrue(detector.isSuspect(3));
    }

    @Test
    public void suspectedProcessShouldEventuallyBecomeCorrect() throws InterruptedException {
        when(mockProcess.getNumberOfProcesses()).thenReturn(2);
        when(mockProcess.getProcessId()).thenReturn(1);
        detector.start();
        sleep(DEFAULT_TIMEOUT_PERIOD + 10);

        assertTrue(detector.isSuspect(2));

        detector.receive(new HeartbeatMessage(2));

        assertFalse(detector.isSuspect(2));
    }

    @Test
    public void shouldNotifyListenersAboutSuspects() throws InterruptedException {
        SuspectListener mockListener = mock(SuspectListener.class);

        when(mockProcess.getNumberOfProcesses()).thenReturn(3);
        when(mockProcess.getProcessId()).thenReturn(1);
        detector.addSuspectListener(mockListener);
        detector.start();

        sleep(DEFAULT_TIMEOUT_PERIOD + 10);

        verify(mockListener, atLeast(1)).suspectsUpdated(Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(2, 3))));

        detector.receive(new HeartbeatMessage(2));
        sleep(10);
        detector.receive(new HeartbeatMessage(2)); //to ensure if the suspect set hasn't changed, nothing gets reported

        verify(mockListener).suspectsUpdated(Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(3))));

        verifyNoMoreInteractions(mockListener);
    }
}
