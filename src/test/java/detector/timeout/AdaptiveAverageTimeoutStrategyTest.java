package detector.timeout;

import message.HeartbeatMessage;
import message.TimestampedMessage;
import org.junit.Test;

import static broker.ActiveMqBroker.DELAY;
import static detector.StronglyCompleteFailureDetector.HEARTBEAT_PERIOD_MS;
import static detector.timeout.TimeoutStrategy.DEFAULT_TIMEOUT_PERIOD;
import static java.lang.Thread.sleep;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Unit test for {@link AdaptiveAverageTimeoutStrategy}.
 */
public class AdaptiveAverageTimeoutStrategyTest {

    @Test
    public void newHistoryShouldReturnDefaultTimeout() {
        assertEquals(DEFAULT_TIMEOUT_PERIOD, new AdaptiveAverageTimeoutStrategy().getNextTimeout());
    }

    @Test
    public void shouldReturnCorrectAverageDelay() throws InterruptedException {
        AdaptiveAverageTimeoutStrategy history = new AdaptiveAverageTimeoutStrategy();

        TimestampedMessage m = new HeartbeatMessage(0);
        sleep(100);
        history.messageReceived(m);

        int approxAvg = HEARTBEAT_PERIOD_MS + 2 * (DELAY + 100) / 2;

        assertTrue(approxAvg + 10 > history.getNextTimeout());
        assertTrue(approxAvg - 10 < history.getNextTimeout());
    }

    @Test
    public void shouldReturnCorrectAverageDelay2() throws InterruptedException {
        AdaptiveAverageTimeoutStrategy history = new AdaptiveAverageTimeoutStrategy();

        TimestampedMessage m = new HeartbeatMessage(0);
        sleep(100);
        history.messageReceived(m);
        sleep(100);
        history.messageReceived(m);
        sleep(100);
        history.messageReceived(m);

        int approxAvg = HEARTBEAT_PERIOD_MS + 2 * (DELAY + 100 + 200 + 300) / 4;

        assertTrue(approxAvg + 10 > history.getNextTimeout());
        assertTrue(approxAvg - 10 < history.getNextTimeout());
    }
}
