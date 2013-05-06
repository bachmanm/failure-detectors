package process.base;

import listener.MessageListener;
import message.HeartbeatMessage;
import message.TimestampedProcessToProcessMessage;
import org.apache.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An {@link Process} which delegates message handling to registered {@link listener.MessageListener}s.
 * <p/>
 * This class is thread-safe.
 */
public abstract class MessageHandlingProcess extends ActiveMqProcess {
    private static final Logger LOG = Logger.getLogger(MessageHandlingProcess.class);

    private final Set<WeakReference<MessageListener>> messageListeners = new CopyOnWriteArraySet<WeakReference<MessageListener>>();

    /**
     * Constructor.
     *
     * @param name              of the process.
     * @param processId         ID of the process, must be consecutive starting with 1, ending with numberOfProcesses.
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public MessageHandlingProcess(String name, int processId, int numberOfProcesses) {
        super(name, processId, numberOfProcesses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(new WeakReference<MessageListener>(messageListener));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliver(TimestampedProcessToProcessMessage message) {
        if (message instanceof HeartbeatMessage) {
            LOG.trace(getName() + " received " + message.toString());
        }
        else {
            LOG.debug(getName() + " received " + message.toString());
        }

        for (WeakReference<MessageListener> reference : messageListeners) {
            MessageListener messageListener = reference.get();
            if (messageListener != null) {
                messageListener.receive(message);
            }
        }
    }
}
