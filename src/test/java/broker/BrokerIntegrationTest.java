package broker;

import listener.MessageListener;
import message.HeartbeatMessage;
import message.TimestampedProcessToProcessMessage;
import message.ValueMessage;
import message.internal.BrokeredMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import process.base.ActiveMqProcess;

import static broker.ActiveMqBroker.DELAY;
import static org.junit.Assert.*;

/**
 * Integration test for {@link broker.FixedDelayBroker}, {@link FailureInjector}, and {@link ActiveMqProcess}es.
 */
public class BrokerIntegrationTest {

    private FixedDelayBroker broker;
    private FailureInjector failureInjector;

    @Before
    public void setUp() {
        setUp(2);
    }

    private void setUp(int numberOfProcesses) {
        broker = new FixedDelayBroker(numberOfProcesses);
        failureInjector = new FailureInjector();
    }

    @After
    public void tearDown() {
        broker.shutdown();
    }

    @Test
    public void processesShouldNotBeReadyIfAllNotStarted() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();

        Thread.sleep(50);

        assertFalse(dummy1.isEverybodyReady());
        assertFalse(dummy2.isEverybodyReady());
    }

    @Test
    public void processesShouldBeReadyAfterAllStarted() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        Thread.sleep(50);

        assertTrue(dummy1.isEverybodyReady());
        assertTrue(dummy2.isEverybodyReady());
    }

    @Test
    public void shouldNotRelayMessagesWhenProcessesNotReady() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();

        Thread.sleep(50);

        dummy1.send(new HeartbeatMessage(1));

        Thread.sleep(DELAY + 50);

        assertNull(dummy1.getMessage());
        assertNull(dummy2.getMessage());
    }

    @Test
    public void shouldBroadcastMessagesWhenProcessesReady() throws InterruptedException {
        tearDown();
        setUp(3);

        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 3);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 3);
        DummyProcess dummy3 = new DummyProcess("Dummy 3", 3, 3);

        dummy1.start();
        dummy2.start();
        dummy3.start();

        Thread.sleep(50);

        dummy1.send(new HeartbeatMessage(1));

        Thread.sleep(DELAY + 50);

        assertNull(dummy1.getMessage()); //not to self!

        assertNotNull(dummy2.getMessage());
        assertTrue(dummy2.getMessage() instanceof HeartbeatMessage);
        assertEquals(1, ((HeartbeatMessage) dummy2.getMessage()).getSource());

        assertNotNull(dummy3.getMessage());
        assertTrue(dummy3.getMessage() instanceof HeartbeatMessage);
        assertEquals(1, ((HeartbeatMessage) dummy3.getMessage()).getSource());
    }

    @Test
    public void shouldUnicastMessagesWhenProcessesReady() throws InterruptedException {
        tearDown();
        setUp(3);

        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 3);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 3);
        DummyProcess dummy3 = new DummyProcess("Dummy 3", 3, 3);

        dummy1.start();
        dummy2.start();
        dummy3.start();

        Thread.sleep(50);

        dummy1.send(new ValueMessage(1, 3, 0, "test"));

        Thread.sleep(DELAY + 50);

        assertNull(dummy1.getMessage());
        assertNull(dummy2.getMessage());

        assertNotNull(dummy3.getMessage());
        assertTrue(dummy3.getMessage() instanceof ValueMessage);
        assertEquals(1, ((ValueMessage) dummy3.getMessage()).getSource());
        assertEquals(0, ((ValueMessage) dummy3.getMessage()).getRound());
        assertEquals("test", ((ValueMessage) dummy3.getMessage()).getValue());
    }

    @Test
    public void shouldNotUnicastMessageFromFailedProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(1);

        Thread.sleep(50);

        dummy1.send(new ValueMessage(1, 2, 0, "test"));

        Thread.sleep(DELAY + 50);

        assertNull(dummy2.getMessage());
    }

    @Test
    public void shouldNotBroadcastMessageFromFailedProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(1);

        Thread.sleep(50);

        dummy1.send(new HeartbeatMessage(1));

        Thread.sleep(DELAY + 50);

        assertNull(dummy2.getMessage());
    }

    @Test
    public void shouldNotUnicastMessageToFailedProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(2);

        Thread.sleep(50);

        dummy1.send(new ValueMessage(1, 2, 0, "test"));

        Thread.sleep(DELAY + 50);

        assertNull(dummy2.getMessage());
    }

    @Test
    public void shouldNotBroadcastMessageToFailedProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(2);

        Thread.sleep(50);

        dummy1.send(new HeartbeatMessage(1));

        Thread.sleep(DELAY + 50);

        assertNull(dummy2.getMessage());
    }

    @Test
    public void shouldUnicastMessageFromRecoveredProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(1);
        Thread.sleep(50);
        failureInjector.restoreProcess(1);
        Thread.sleep(50);

        dummy1.send(new ValueMessage(1, 2, 0, "test"));

        Thread.sleep(DELAY + 50);

        assertNotNull(dummy2.getMessage());
    }

    @Test
    public void shouldBroadcastMessageFromRecoveredProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(1);
        Thread.sleep(50);
        failureInjector.restoreProcess(1);
        Thread.sleep(50);

        Thread.sleep(50);

        dummy1.send(new HeartbeatMessage(1));

        Thread.sleep(DELAY + 50);

        assertNotNull(dummy2.getMessage());
    }

    @Test
    public void shouldUnicastMessageToRecoveredProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(2);
        Thread.sleep(50);
        failureInjector.restoreProcess(2);
        Thread.sleep(50);

        Thread.sleep(50);

        dummy1.send(new ValueMessage(1, 2, 0, "test"));

        Thread.sleep(DELAY + 50);

        assertNotNull(dummy2.getMessage());
    }

    @Test
    public void shouldBroadcastMessageToRecoveredProcess() throws InterruptedException {
        DummyProcess dummy1 = new DummyProcess("Dummy 1", 1, 2);
        DummyProcess dummy2 = new DummyProcess("Dummy 2", 2, 2);

        dummy1.start();
        dummy2.start();

        failureInjector.killProcess(2);
        Thread.sleep(50);
        failureInjector.restoreProcess(2);
        Thread.sleep(50);

        Thread.sleep(50);

        dummy1.send(new HeartbeatMessage(1));

        Thread.sleep(DELAY + 50);

        assertNotNull(dummy2.getMessage());
    }

    private class DummyProcess extends ActiveMqProcess {

        private volatile boolean everybodyReady = false;
        private volatile BrokeredMessage message;

        private DummyProcess(String name, int processId, int numberOfProcesses) {
            super(name, processId, numberOfProcesses);
        }

        @Override
        protected void everybodyReady() {
            everybodyReady = true;
        }

        private boolean isEverybodyReady() {
            return everybodyReady;
        }

        @Override
        public void deliver(TimestampedProcessToProcessMessage message) {
            this.message = message;
        }

        private BrokeredMessage getMessage() {
            return message;
        }

        @Override
        public void addMessageListener(MessageListener messageListener) {
        }
    }
}
