package detector;

import listener.SuspectListener;
import org.apache.log4j.Logger;

import java.util.Set;

/**
 * An extension of the {@link EventuallyPerfectFailureDetector} that is capable of eventually electing a leader.
 * <p/>
 * The election is triggered by a change in the list of suspects. The process with largest ID that is correct is elected leader.
 * <p/>
 * This class is thread-safe, all non-private methods are synchronized.
 */
public class LeaderElectingEventuallyPerfectFailureDetector extends EventuallyPerfectFailureDetector implements SuspectListener {
    private static final Logger LOG = Logger.getLogger(LeaderElectingEventuallyPerfectFailureDetector.class);

    private int currentLeader;

    /**
     * Constructor.
     *
     * @param process to which this failure detector belongs.
     */
    public LeaderElectingEventuallyPerfectFailureDetector(process.base.Process process) {
        super(process);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Register itself to be updated when suspects change and trigger initial leader election.
     */
    @Override
    public synchronized void start() {
        super.start();
        addSuspectListener(this);
        electNewLeader(getSuspects());
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Elect a new leader when suspects changed.
     */
    @Override
    public synchronized void suspectsUpdated(Set<Integer> suspects) {
        electNewLeader(suspects);
    }

    /**
     * Elect a new leader.
     *
     * @param suspects currently suspected processes.
     */
    private void electNewLeader(Set<Integer> suspects) {
        int newLeader;
        for (newLeader = process.getNumberOfProcesses(); newLeader > 0; newLeader--) {
            if (!suspects.contains(newLeader)) {
                break;
            }
        }

        if (currentLeader != newLeader) {
            LOG.info(process.getName() + " elected a new leader: " + newLeader);
            currentLeader = newLeader;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int getLeader() {
        return currentLeader;
    }
}
