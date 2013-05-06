package process.base;

import listener.MessageListener;
import message.internal.ProcessMessage;
import message.ProcessToProcessMessage;
import message.TimestampedProcessToProcessMessage;

import java.util.concurrent.Future;

/**
 * A process that communicates with other processes in order to achieve something in a distributed system.
 */
public interface Process {

    /**
     * Start the process.
     */
    void start();

    /**
     * Stop the process.
     */
    void stop();

    /**
     * Send a message.
     *
     * @param message to send.
     */
    void send(ProcessMessage message);

    /**
     * Deliver, i.e. receive a message.
     *
     * @param message received message.
     */
    void deliver(TimestampedProcessToProcessMessage message);

    /**
     * Register a listener for message deliveries.
     *
     * @param messageListener to register.
     */
    void addMessageListener(MessageListener messageListener);

    /**
     * Get this process' name.
     *
     * @return the process' name.
     */
    String getName();

    /**
     * Get this process' ID.
     *
     * @return the process' ID.
     */
    int getProcessId();

    /**
     * Get total number of processes in the ensemble.
     *
     * @return total number of processes.
     */
    int getNumberOfProcesses();
}
