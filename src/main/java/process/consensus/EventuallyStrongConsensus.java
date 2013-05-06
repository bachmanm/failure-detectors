package process.consensus;

import detector.EventuallyStrongFailureDetector;
import listener.MessageListener;
import listener.SuspectListener;
import message.OutcomeMessage;
import message.TimestampedProcessToProcessMessage;
import message.ValueMessage;
import org.apache.log4j.Logger;
import process.base.Process;
import process.consensus.value.*;
import util.MapSorter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A class capable of reaching a consensus using/assuming an eventually strong failure detector.
 * There must be a maximum of N/3 faulty processes.
 * <p/>
 * This class is designed to be run in a separate thread because it contains some blocking operations, typically when
 * collecting messages from a processes. These get unblocked when a message is received or when a process becomes suspect.
 * <p/>
 * By implementing {@link Callable}, this class is designed to be submitted to an {@link java.util.concurrent.ExecutorService}.
 * <p/>
 * A new instance of the class should be created every time a consensus-reaching algorithm is launched.
 * <p/>
 * This class is thread-safe, all non-private methods are synchronized.
 */
public class EventuallyStrongConsensus implements Callable<String>, SuspectListener, MessageListener {
    private static final Logger LOG = Logger.getLogger(EventuallyStrongConsensus.class);

    private final Process process;
    private final EventuallyStrongFailureDetector detector;

    private String currentProposal;
    private int currentRound = 0;
    private int m = 0;

    /**
     * Proposals collected in each round, keyed by round number
     */
    private final Map<Integer, Value[]> collectedProposals = new HashMap<Integer, Value[]>();

    /**
     * Outcome collected in each round, keyed by round number
     */
    private final Map<Integer, Value> collectedOutcome = new HashMap<Integer, Value>();

    /**
     * Create a new instance to solve a consensus.
     *
     * @param process         process this consensus instance belongs to.
     * @param detector        failure detector of the process this consensus instance belongs to.
     *                        This should be an eventually strong failure detector.
     * @param initialProposal value initially proposed by this instance.
     * @return consensus-reaching callable.
     */
    public static EventuallyStrongConsensus createInstance(Process process, EventuallyStrongFailureDetector detector, String initialProposal) {
        EventuallyStrongConsensus instance = new EventuallyStrongConsensus(process, detector, initialProposal);
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
     *                        This should be an eventually strong failure detector.
     * @param initialProposal value initially proposed by this instance.
     */
    private EventuallyStrongConsensus(Process process, EventuallyStrongFailureDetector detector, String initialProposal) {
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
        while (true) {
            initializeNewRound();
            everyoneSendProposalToCoordinator();                //Step 1: Everyone sends their proposal to the current coordinator
            coordinatorCollectProposalsAndBroadcastOutcome();   //Step 2: Coordinator collects the proposals and broadcasts the outcome
            everyoneCollectOutcomeFromCoordinator();            //Step 3: Everyone collects the outcome from the coordinator
            if (decideAndTerminate()) return currentProposal;   //Step 4: Decision, termination
        }
    }

    /**
     * Increment the current round number and initialize placeholders for value collection.
     */
    private void initializeNewRound() {
        currentRound++;
        if (isCurrentCoordinator()) {
            initializeCollectedProposals(currentRound);
        }
        initializeCollectedOutcome(currentRound);
    }

    /**
     * Everyone sends their proposal to current coordinator. The coordinator itself just knows about it.
     */
    private void everyoneSendProposalToCoordinator() {
        if (isCurrentCoordinator()) {
            sendProposalToSelf();
        } else {
            sendProposalToCoordinator();
        }
    }

    /**
     * The current coordinator collects enough proposals and broadcasts the outcome.
     *
     * @throws InterruptedException
     */
    private void coordinatorCollectProposalsAndBroadcastOutcome() throws InterruptedException {
        if (isCurrentCoordinator()) {
            accountForAlreadyKnownSuspects();

            while (!hasEnoughProposals()) {
                wait(); //block current thread until enough proposals have been collected
            }

            //deviation from the theoretical algorithm: broadcasting instead of unicasting to all those
            //who have managed to send a proposal. This way, the correct processes that also sent a proposal but
            //too late for the coordinator to care also get the outcome.
            broadcastOutcome();
        }
    }

    /**
     * Everyone collects the outcome from the coordinator, or suspects it.
     *
     * @throws InterruptedException
     */
    private void everyoneCollectOutcomeFromCoordinator() throws InterruptedException {
        accountForAlreadyKnownSuspects();
        while (collectedOutcome.get(currentRound).isUnknown()) {
            wait();
        }
    }

    /**
     * Try to make a decision based on the outcome and decide whether the consensus-reaching process should terminate.
     *
     * @return true if and only if the process should terminate.
     */
    private boolean decideAndTerminate() {
        if (collectedOutcome.get(currentRound).isValid()) {
            currentProposal = ((ValidValue) collectedOutcome.get(currentRound)).getValue();
            if (((Outcome) collectedOutcome.get(currentRound)).isUnanimous()) {
                LOG.info(process.getName() + " decided " + currentProposal + " in round " + currentRound);

                //terminate?
                if (m == getCurrentCoordinator()) {
                    LOG.info(process.getName() + " terminating (decision = " + currentProposal + ") in round " + currentRound);
                    return true;
                } else if (m == 0) {
                    m = getCurrentCoordinator();
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * In case the process from which a message is currently expected became suspect, collected value or outcome
     * is updated to represent this fact and blocked threads notified.
     */
    @Override
    public synchronized void suspectsUpdated(Set<Integer> suspects) {
        if (isCurrentCoordinator()) {
            for (Integer suspect : suspects) {
                if (collectedProposals.get(currentRound)[suspect - 1].equals(UnknownValue.getInstance())) {
                    collectedProposals.get(currentRound)[suspect - 1] = SuspectValue.getInstance();
                }
            }
            notifyAll();
        }

        if (suspects.contains(getCurrentCoordinator())) {
            if (collectedOutcome.get(currentRound).equals(UnknownValue.getInstance())) {
                collectedOutcome.put(currentRound, SuspectValue.getInstance());
            }
            notifyAll();
        }

    }

    /**
     * {@inheritDoc}
     * <p/>
     * In an expected message has been delivered, collected value/outcome is updated to represent this fact
     * and blocked threads notified.
     */
    @Override
    public synchronized void receive(TimestampedProcessToProcessMessage message) {
        if (message instanceof OutcomeMessage) {
            Outcome outcome = Outcome.fromMessage((OutcomeMessage) message);
            initializeCollectedOutcome(outcome.getRound()); //in case it has not yet been initialized (the sender is ahead of the game)
            collectedOutcome.put(outcome.getRound(), outcome);
            notifyAll();
        } else if (message instanceof ValueMessage) {
            ValidValue validValue = ValidValue.fromMessage((ValueMessage) message);
            initializeCollectedProposals(validValue.getRound()); //in case it has not yet been initialized (the sender is ahead of the game)
            collectedProposals.get(validValue.getRound())[message.getSource() - 1] = validValue;
            notifyAll();
        }
    }

    /**
     * Initialize collected proposals for the given round to unknown values, if they haven't already been initialized.
     *
     * @param currentRound round number to initialize collected values for.
     */
    private void initializeCollectedProposals(int currentRound) {
        if (collectedProposals.containsKey(currentRound)) {
            return;  //already initialized
        }

        Value[] collectedValues = new Value[process.getNumberOfProcesses()];
        for (int i = 0; i < collectedValues.length; i++) {
            collectedValues[i] = UnknownValue.getInstance();
        }
        this.collectedProposals.put(currentRound, collectedValues);
    }

    /**
     * Initialize collected outcome for the given round to an unknown value, if it hasn't already been initialized.
     *
     * @param currentRound round number to initialize collected outcome for.
     */
    private void initializeCollectedOutcome(int currentRound) {
        if (collectedOutcome.containsKey(currentRound)) {
            return;  //already initialized
        }

        collectedOutcome.put(currentRound, UnknownValue.getInstance());
    }

    /**
     * Account for already known suspects by triggering a fake suspect update event.
     */
    private void accountForAlreadyKnownSuspects() {
        suspectsUpdated(detector.getSuspects());
    }

    /**
     * Is the owning process the current coordinator?
     *
     * @return true if and only if the current process is the coordinator.
     */
    private boolean isCurrentCoordinator() {
        return process.getProcessId() == getCurrentCoordinator();
    }

    /**
     * Get the ID of the process that is the current coordinator for this round.
     *
     * @return current coordinator process ID.
     */
    private int getCurrentCoordinator() {
        return (currentRound - 1) % process.getNumberOfProcesses() + 1;
    }

    /**
     * Pretend to be sending the proposal to itself just by collecting the proposal straight away.
     */
    private void sendProposalToSelf() {
        //If I'm currently the coordinator, I collect the value rather than sending it to myself.
        collectedProposals.get(currentRound)[process.getProcessId() - 1] = new ValidValue(currentProposal, currentRound);
    }

    /**
     * Unicast the current proposal to the current coordinator.
     */
    private void sendProposalToCoordinator() {
        process.send(new ValidValue(currentProposal, currentRound).toMessage(process.getProcessId(), getCurrentCoordinator()));
    }

    /**
     * Has this process collected enough proposals? Enough for this implementation is at least N - F, where N is the
     * total number of processes in the ensemble and F is the maximum number of faulty processes that this algorithm can
     * tolerate, which is strictly less than N/3.
     *
     * @return true if and only if enough proposals have been collected.
     */
    private boolean hasEnoughProposals() {
        int validProposals = 0;
        int maxFaultyProcesses = (int) Math.floor(process.getNumberOfProcesses() * 1.0 / 3.0);
        int minProposals = process.getNumberOfProcesses() - maxFaultyProcesses; //F

        for (Value proposal : collectedProposals.get(currentRound)) {
            if (proposal.isValid()) {
                validProposals++;
            }
        }

        return validProposals >= minProposals;
    }

    /**
     * Broadcast the majority proposal to all the processes.
     */
    private void broadcastOutcome() {
        String majorityProposal = findMajorityProposal();
        boolean unanimous = allProposalsSameAs(majorityProposal);

        Outcome outcome = new Outcome(unanimous, majorityProposal, currentRound);
        collectedOutcome.put(currentRound, outcome); //I don't want to send a message to myself, so I'll just pretend I've already collected it.
        process.send(outcome.toMessage(process.getProcessId(), -1));
    }

    /**
     * Find the majority proposal.
     *
     * @return the majority. If there is a draw/tie, an (undefined) one from the proposals is returned.
     */
    private String findMajorityProposal() {
        Map<String, Integer> proposalCounts = new HashMap<String, Integer>();
        for (Value proposal : collectedProposals.get(currentRound)) {
            if (proposal.isValid()) {
                String proposedValue = ((ValidValue) proposal).getValue();
                if (!proposalCounts.containsKey(proposedValue)) {
                    proposalCounts.put(proposedValue, 0);
                }
                proposalCounts.put(proposedValue, proposalCounts.get(proposedValue) + 1);
            }
        }

        return MapSorter.sortMapByDescendingValue(proposalCounts).firstKey();
    }

    /**
     * Are all the collected valid proposals same as the given value?
     *
     * @param value to compare proposals to.
     * @return true if and only if all collected proposals have the same value as the given one.
     */
    private boolean allProposalsSameAs(String value) {
        for (Value proposal : collectedProposals.get(currentRound)) {
            if (proposal.isValid() && !((ValidValue) proposal).getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }
}
