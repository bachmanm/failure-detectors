package listener;


import message.TimestampedProcessToProcessMessage;

/**
 * Interface for listeners that wish to be notified when new messages are delivered to a process.
 */
public interface MessageListener {

    /**
     * Handle a new message.
     *
     * @param message the delivered message.
     */
    void receive(TimestampedProcessToProcessMessage message);
}
