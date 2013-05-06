package message;

import message.internal.ProcessMessage;

/**
 * Message from a process to another process (or to all processes, i.e. broadcast).
 */
public interface ProcessToProcessMessage extends ProcessMessage {

    /**
     * Get the destination of the message.
     *
     * @return process ID of the destination, or {@link broker.ActiveMqBroker#BROADCAST_DESTINATION} to indicate broadcast.
     */
    int getDestination();
}
