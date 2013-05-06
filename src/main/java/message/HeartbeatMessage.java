package message;

import static broker.ActiveMqBroker.*;

/**
 * A heartbeat message that is broadcast.
 */
public class HeartbeatMessage extends TimestampedProcessToProcessMessage {

    /**
     * Construct a new heartbeat to be broadcast.
     *
     * @param source ID of the source process.
     */
    public HeartbeatMessage(int source) {
        super(source, BROADCAST_DESTINATION);
    }

    @Override
    public String toString() {
        return "HB:" + getSource();
    }
}
