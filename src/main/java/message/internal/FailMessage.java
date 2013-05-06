package message.internal;

/**
 * A control message used to tell a broker that it should simulate a process failure.
 */
public class FailMessage implements BrokeredMessage {

    private final int processId;

    /**
     * Construct a new message.
     *
     * @param processId ID of the failed process.
     */
    public FailMessage(int processId) {
        this.processId = processId;
    }

    /**
     * Get ID of the failed process.
     *
     * @return process ID.
     */
    public int getProcessId() {
        return processId;
    }
}
