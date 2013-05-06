package process.base;

import message.TimestampedProcessToProcessMessage;
import message.internal.ProcessMessage;
import message.internal.ReadyMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.log4j.Logger;

import javax.jms.*;

import java.io.Serializable;

import static broker.ActiveMqBroker.*;

/**
 * A {@link Process} that uses ActiveMQ to communicate with the broker.
 */
public abstract class ActiveMqProcess extends BaseProcess implements MessageListener {
    private static final Logger LOG = Logger.getLogger(ActiveMqProcess.class);

    private MessageProducer messageProducer;
    private Connection connection;
    private Session session;

    /**
     * Create a new process.
     *
     * @param name              process name.
     * @param processId         process ID.
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public ActiveMqProcess(String name, int processId, int numberOfProcesses) {
        super(name, processId, numberOfProcesses);

        createConnectionAndSession();
        setupMessageProducer();
        setupMessageConsumer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(ProcessMessage message) {
        ActiveMQObjectMessage activeMQObjectMessage = new ActiveMQObjectMessage();
        try {
            activeMQObjectMessage.setObject(message);
            messageProducer.send(activeMQObjectMessage);
        } catch (JMSException e) {
            LOG.error("Failed to send message!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message message) {
        if (!(message instanceof ActiveMQObjectMessage)) {
            LOG.error("Incompatible message received! Only ActiveMQObjectMessage supported.");
            return;
        }
        try {
            Serializable wrappedMessage = ((ActiveMQObjectMessage) message).getObject();
            if (wrappedMessage instanceof ReadyMessage) {
                everybodyReady();
                return;
            }
            if (!(wrappedMessage instanceof TimestampedProcessToProcessMessage)) {
                LOG.error("Incompatible message received! Only TimestampedProcessToProcessMessage supported.");
                return;
            }
            deliver((TimestampedProcessToProcessMessage) wrappedMessage);
        } catch (JMSException e) {
            LOG.error("Failed to receive message!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void start() {
        send(new ReadyMessage(getProcessId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        try {
            session.close();
            connection.close();
        } catch (JMSException e) {
            LOG.error("Failed to stop process!", e);
        }
    }

    /**
     * Act upon the fact that the broker and all processes are ready.
     */
    protected abstract void everybodyReady();

    private void createConnectionAndSession() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            LOG.error("Failed to create JMS session!", e);
        }
    }

    private void setupMessageProducer() {
        try {
            Destination fromProcessQueue = session.createQueue(FROM_PROCESS_QUEUE);
            messageProducer = session.createProducer(fromProcessQueue);
        } catch (JMSException e) {
            LOG.error("Failed to setup message producer!", e);
        }
    }

    private void setupMessageConsumer() {
        try {
            Destination toProcessQueue = session.createQueue(TO_PROCESS_QUEUE + getProcessId());
            MessageConsumer consumer = session.createConsumer(toProcessQueue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            LOG.error("Failed to setup message consumer!", e);
        }
    }
}
