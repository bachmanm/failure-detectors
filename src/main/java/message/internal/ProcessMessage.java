package message.internal;

/**
 * Message originated by a process.
 */
public interface ProcessMessage extends BrokeredMessage {

    /**
     * Get source of the message.
     *
     * @return ID of the originating process. Can be 0 in case the originator is the broker (e.g. when sending a control message).
     */
    int getSource();
}
