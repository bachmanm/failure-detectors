package broker;

import message.internal.FailMessage;
import message.internal.ResurrectMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.log4j.Logger;

import javax.jms.*;

import static broker.ActiveMqBroker.*;

/**
 *
 */
public class FailureInjector {
    private static final Logger LOG = Logger.getLogger(FailureInjector.class);

    private MessageProducer producer;

    public FailureInjector() {
        setupMessageProducer();
    }

    private void setupMessageProducer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination toProcessQueue = session.createQueue(FROM_PROCESS_QUEUE);
            producer = session.createProducer(toProcessQueue);
            producer.setDeliveryMode(Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            LOG.fatal("Failed to create message producer.", e);
        }
    }

    public void killProcess(int processId) {
        ActiveMQObjectMessage activeMQObjectMessage = new ActiveMQObjectMessage();
        try {
            activeMQObjectMessage.setObject(new FailMessage(processId));
            producer.send(activeMQObjectMessage);
        } catch (JMSException e) {
            LOG.error("Error sending message!", e);
        }
    }

    public void restoreProcess(int processId) {
        ActiveMQObjectMessage activeMQObjectMessage = new ActiveMQObjectMessage();
        try {
            activeMQObjectMessage.setObject(new ResurrectMessage(processId));
            producer.send(activeMQObjectMessage);
        } catch (JMSException e) {
            LOG.error("Error sending message!", e);
        }
    }
}
