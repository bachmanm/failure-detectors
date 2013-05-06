package detector.timeout;

import message.HeartbeatMessage;
import message.TimestampedMessage;
import org.junit.Test;

import static broker.ActiveMqBroker.DELAY;
import static detector.StronglyCompleteFailureDetector.HEARTBEAT_PERIOD_MS;
import static java.lang.Thread.sleep;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Unit test for {@link detector.timeout.AdaptiveMaxTimeoutStrategy}.
 */
public class AdaptiveMaxTimeoutStrategyTest {

    @Test
    public void newHistoryShouldReturnDefaultTimeout() {
        assertEquals(HEARTBEAT_PERIOD_MS + DELAY, new AdaptiveMaxTimeoutStrategy().getNextTimeout());
    }

    @Test
    public void shouldReturnCorrectMaxDelay() throws InterruptedException {
        AdaptiveMaxTimeoutStrategy history = new AdaptiveMaxTimeoutStrategy();

        TimestampedMessage m = new HeartbeatMessage(0);
        sleep(200);
        history.messageReceived(m);

        int approxMax = HEARTBEAT_PERIOD_MS + 200;

        assertTrue(approxMax + 10 > history.getNextTimeout());
        assertTrue(approxMax - 10 < history.getNextTimeout());
    }

    @Test
    public void shouldReturnCorrectMaxDelay2() throws InterruptedException {
        AdaptiveMaxTimeoutStrategy history = new AdaptiveMaxTimeoutStrategy();

        TimestampedMessage m = new HeartbeatMessage(0);
        sleep(100);
        history.messageReceived(m);
        sleep(100);
        history.messageReceived(m);
        sleep(100);
        history.messageReceived(m);

        int approxMax = HEARTBEAT_PERIOD_MS + 300;

        assertTrue(approxMax + 10 > history.getNextTimeout());
        assertTrue(approxMax - 10 < history.getNextTimeout());
    }
}
