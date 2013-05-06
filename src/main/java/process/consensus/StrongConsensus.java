package process.consensus;

import detector.StrongFailureDetector;
import listener.MessageListener;
import listener.SuspectListener;
import message.TimestampedProcessToProcessMessage;
import message.ValueMessage;
import org.apache.log4j.Logger;
import process.base.Process;
import process.consensus.value.SuspectValue;
import process.consensus.value.UnknownValue;
import process.consensus.value.ValidValue;
import process.consensus.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A class capable of reaching a consensus using a strong failure detector. There must be at least one correct process.
 * <p/>
 * This class is designed to be run in a separate thread because it contains some blocking operations, typically when
 * collecting messages from a process. These get unblocked when a message is received or when a process becomes suspect.
 * <p/>
 * By implementing {@link Callable}, this class is designed to be submitted to an {@link java.util.concurrent.ExecutorService}.
 * <p/>
 * A new instance of the class should be created every time a consensus-reaching algorithm is launched.
 * <p/>
 * This class is thread-safe, all non-private methods are synchronized.
 */
public class StrongConsensus implements Callable<String>, SuspectListener, MessageListener {
    private static final Logger LOG = Logger.getLogger(StrongConsensus.class);

    private final Process process;
    private final StrongFailureDetector detector;

    private String currentProposal; //x
    private int currentRound;       //r

    /**
     * Proposal collected in each round, keyed by round number
     */
    private final Map<Integer, Value> collectedProposal = new HashMap<Integer, Value>();

    /**
     * Create a new instance to solve a consensus.
     *
     * @param process         process this consensus instance belongs to.
     * @param detector        failure detector of the process this consensus instance belongs to.
     *                        This should be a strong failure detector.
     * @param initialProposal value initially proposed by this instance.
     * @return consensus-reaching callable.
     */
    public static StrongConsensus createInstance(Process process, StrongFailureDetector detector, String initialProposal) {
        StrongConsensus instance = new StrongConsensus(process, detector, initialProposal);
        detector.addSuspectListener(instance);
        process.addMessageListener(instance);
        return instance;
    }

    /**
     * Private constructor to prevent instantiation, please use the above factory method.
     * The reason for this is listener registrations. Since this is a multi-threaded system, we don't want to leak
     * the reference to this object before it has been fully instantiated (i.e. from the constructor).
     *
     * @param process         process this consensus instance belongs to.
     * @param detector        failure detector of the process this consensus instance belongs to.
     *                        This should be a strong failure detector.
     * @param initialProposal value initially proposed by this instance.
     */
    private StrongConsensus(Process process, StrongFailureDetector detector, String initialProposal) {
        this.process = process;
        this.detector = detector;
        this.currentProposal = initialProposal;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Starts the consensus process.
     */
    @Override
    public synchronized String call() throws Exception {
        initializeCollectedProposals();
        for (currentRound = 1; currentRound <= process.getNumberOfProcesses(); currentRound++) {
            if (isCurrentCoordinator()) {
                broadcastCurrentProposal();
                collectedProposal.put(currentRound, new ValidValue(currentProposal, currentRound)); //coordinator pretends to have collected the value rather than sending it to itself.
            }

            suspectsUpdated(detector.getSuspects()); //account for any processes already suspected

            while (collectedProposal.get(currentRound).isUnknown()) {
                wait(); //block current thread until a value has been collected
            }

            if (collectedProposal.get(currentRound).isValid()) { //the other option than valid is that the process became a suspect
                currentProposal = ((ValidValue) collectedProposal.get(currentRound)).getValue();
            }
        }

        LOG.info(process.getName() + " decided " + currentProposal);
        return currentProposal;
    }

    /**
     * Initialize the collected proposals by setting them to unknown for each round.
     */
    private void initializeCollectedProposals() {
        for (int round = 1; round <= process.getNumberOfProcesses(); round++) {
            collectedProposal.put(round, UnknownValue.getInstance());
        }
    }

    /**
     * Is the owning process the coordinator of this round?
     *
     * @return true if and only if the current round number equals ID of this process.
     */
    private boolean isCurrentCoordinator() {
        return currentRound == process.getProcessId();
    }

    /**
     * Broadcast the current proposal to all other processes.
     */
    private void broadcastCurrentProposal() {
        process.send(new ValidValue(currentProposal, currentRound).toMessage(process.getProcessId(), -1));
    }

    /**
     * {@inheritDoc}
     * <p/>
     * In case a process became suspect, collected value is updated to represent this fact and blocked threads notified.
     */
    @Override
    public synchronized void suspectsUpdated(Set<Integer> suspects) {
        for (Integer suspect : suspects) {
            collectedProposal.put(suspect, SuspectValue.getInstance());
        }
        notifyAll();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * In an expected message has been delivered, collected value is updated to represent this fact and blocked threads notified.
     */
    @Override
    public synchronized void receive(TimestampedProcessToProcessMessage message) {
        if (message instanceof ValueMessage) {
            if (collectedProposal.get(message.getSource()).isUnknown()) {
                collectedProposal.put(message.getSource(), ValidValue.fromMessage((ValueMessage) message));
                notifyAll();
            }
        }
    }
}
