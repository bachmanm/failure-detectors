package broker;

import message.internal.FailMessage;
import message.internal.ProcessMessage;
import message.ProcessToProcessMessage;
import message.internal.ReadyMessage;
import message.internal.ResurrectMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Base class for brokers using ActiveMQ.
 * <p/>
 * All messages in the system are relayed through this broker, so that it can simulate message delays / process failures.
 * This is achieved by consuming messages on the {@link ActiveMqBroker#FROM_PROCESS_QUEUE}. Then, they can either
 * be dropped (to simulate a process failure), or delayed and then sent onto the correct
 * {@link ActiveMqBroker#TO_PROCESS_QUEUE}, i.e. the queue that belongs to the destination process. Please note that
 * this entire simulation assumes no link failures.
 * <p/>
 * Processes are numbered 1 to N. The destination of each message is either one of these process IDs, or {@link ActiveMqBroker#BROADCAST_DESTINATION},
 * which means "broadcast". Messages are broadcast to all processes except for the sender.
 * <p/>
 * Before anything starts happening, a {@link ReadyMessage} must be received from all processes. After that, a {@link ReadyMessage}
 * is sent back to all the processes, at which point the simulation can start.
 */
public abstract class ActiveMqBroker implements MessageListener, Broker {
    private static final Logger LOG = Logger.getLogger(ActiveMqBroker.class);

    public static final String BROKER_URL = "tcp://localhost:61617";
    public static final String FROM_PROCESS_QUEUE = "from.process";
    public static final String TO_PROCESS_QUEUE = "to.process.";
    public static final int BROADCAST_DESTINATION = -1;
    private static final int SENDING_THREADS = 10;

    public static final int DELAY = 100;

    private BrokerService broker;

    /**
     * Executors for scheduled delivery to destination processes, after a delay.
     */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(SENDING_THREADS);

    /**
     * Message producers, one per process, indexed by processId - 1
     */
    private MessageProducer[] messageProducers;

    /**
     * The total number of processes in the ensemble
     */
    protected final int totalNumberOfProcesses;

    /**
     * IDs processes that are ready.
     */
    private final Set<Integer> ready = new ConcurrentSkipListSet<Integer>();

    /**
     * IDs processes that have failed.
     */
    private final Set<Integer> failed = new ConcurrentSkipListSet<Integer>();

    /**
     * Construct a new broker.
     *
     * @param totalNumberOfProcesses total number of processes in the ensemble.
     */
    public ActiveMqBroker(int totalNumberOfProcesses) {
        this.totalNumberOfProcesses = totalNumberOfProcesses;
        createActiveMqBroker();
        setupMessageQueueConsumer();
        setupMessageProducers();
    }

    @Override
    public void shutdown() {
        try {
            broker.stop();
        } catch (Exception e) {
            LOG.error("Failed stopping broker", e);
        }
    }

    private void createActiveMqBroker() {
        try {
            broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.addConnector(BROKER_URL);
            broker.setUseShutdownHook(true);
            broker.start();
        } catch (Exception e) {
            LOG.fatal("Failed to start ActiveMQ broker.", e);
        }
    }

    private void setupMessageQueueConsumer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination fromProcessQueue = session.createQueue(FROM_PROCESS_QUEUE);
            MessageConsumer consumer = session.createConsumer(fromProcessQueue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            LOG.fatal("Failed to create message queue consumer.", e);
        }
    }

    private void setupMessageProducers() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            messageProducers = new MessageProducer[totalNumberOfProcesses];
            for (int i = 1; i <= totalNumberOfProcesses; i++) {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination toProcessQueue = session.createQueue(TO_PROCESS_QUEUE + i);
                MessageProducer producer = session.createProducer(toProcessQueue);
                producer.setDeliveryMode(Session.AUTO_ACKNOWLEDGE);
                messageProducers[i - 1] = producer;
            }
        } catch (JMSException e) {
            LOG.fatal("Failed to create message producers.", e);
        }
    }

    private void ready(int processId) {
        if (ready.contains(processId)) {
            LOG.warn("Something is wrong: process " + processId + " said that it is ready more than once!");
        }

        ready.add(processId);

        if (ready.size() > totalNumberOfProcesses) {
            LOG.warn("Something is wrong: the total number of ready processes (" + ready.size() + ") " +
                    "is greater than the total number of processes (" + totalNumberOfProcesses + ")!");
        }

        if (ready.size() == totalNumberOfProcesses) {
            LOG.info("All processes are ready.");
            broadcastNow(new ReadyMessage(0));
        }
    }

    private boolean allReady() {
        return totalNumberOfProcesses <= ready.size();
    }

    private void fail(int processId) {
        LOG.info("Killing process " + processId);
        failed.add(processId);
    }

    private void recover(int processId) {
        LOG.info("Resurrecting process " + processId);
        failed.remove(processId);
    }

    private boolean hasFailed(int processId) {
        return failed.contains(processId);
    }

    @Override
    public void onMessage(Message message) {
        if (!(message instanceof ObjectMessage)) {
            LOG.error("Unknown message type received by broker!");
            return;
        }

        ObjectMessage msg = (ObjectMessage) message;
        try {
            Serializable wrappedMessage = msg.getObject();
            if (wrappedMessage instanceof ReadyMessage) {
                handleReadyMessage((ReadyMessage) wrappedMessage);
            } else if (wrappedMessage instanceof ProcessToProcessMessage) {
                handleProcessToProcessMessage((ProcessToProcessMessage) wrappedMessage);
            } else if (wrappedMessage instanceof FailMessage) {
                handleFailMessage((FailMessage) wrappedMessage);
            } else if (wrappedMessage instanceof ResurrectMessage) {
                handleResurrectMessage((ResurrectMessage) wrappedMessage);
            }
        } catch (JMSException e) {
            LOG.error("Error receiving message!", e);
        }
    }

    private void handleReadyMessage(ReadyMessage message) throws JMSException {
        ready(message.getSource());
    }

    private void handleProcessToProcessMessage(ProcessToProcessMessage message) throws JMSException {
        if (allReady()) {
            if (message.getDestination() == BROADCAST_DESTINATION) {
                broadcast(message);
            } else {
                unicast(message, message.getDestination());
            }
        }
    }

    private void handleFailMessage(FailMessage message) throws JMSException {
        fail(message.getProcessId());
    }

    private void handleResurrectMessage(ResurrectMessage message) throws JMSException {
        recover(message.getProcessId());
    }

    private void broadcast(final ProcessToProcessMessage p2pMessage) {
        for (int i = 1; i <= totalNumberOfProcesses; i++) {
            if (i != p2pMessage.getSource()) { //don't send to self
                unicast(p2pMessage, i);
            }
        }
    }

    //for system messages that don't need to be delayed.
    private void broadcastNow(final ProcessMessage p2pMessage) {
        for (int i = 1; i <= totalNumberOfProcesses; i++) {
            unicastNow(p2pMessage, i);
        }
    }

    private void unicast(final ProcessMessage p2pMessage, final int destination) {
        if (hasFailed(destination) || hasFailed(p2pMessage.getSource())) {
            return;
        }

        executor.schedule(new Runnable() {
            @Override
            public void run() {
                unicastNow(p2pMessage, destination);
            }
        }, getDelay(), TimeUnit.MILLISECONDS);
    }

    //for system messages that don't need to be delayed.
    private void unicastNow(final ProcessMessage p2pMessage, int destination) {
        ActiveMQObjectMessage activeMQObjectMessage = new ActiveMQObjectMessage();
        try {
            activeMQObjectMessage.setObject(p2pMessage);
            messageProducers[destination - 1].send(activeMQObjectMessage);
        } catch (JMSException e) {
            LOG.error("Error sending message!", e);
        }
    }

    /**
     * Compute delay for the next message.
     *
     * @return delay in ms.
     */
    protected abstract long getDelay();
}
