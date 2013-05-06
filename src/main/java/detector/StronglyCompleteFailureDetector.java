package detector;

import detector.timeout.TimeoutStrategy;
import listener.MessageListener;
import listener.SuspectListener;
import message.HeartbeatMessage;
import message.TimestampedProcessToProcessMessage;
import org.apache.log4j.Logger;
import process.base.Process;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Base class for various strongly complete {@link FailureDetector} implementations.
 * <p/>
 * This class uses the template design pattern. It is abstract and delegates certain responsibilities to subclasses,
 * while handling the plumbing.
 * <p/>
 * Specifically, this class is responsible for:
 * <ul>
 * <li>Listening to messages and delegating parts of message handling to subclasses.</li>
 * <li>Scheduling processes for suspicion for purposes of failure detection letting a concrete implementation of
 * {@link TimeoutStrategy} to determine the timeout.</li>
 * <li>Broadcasting periodic heartbeats.</li>
 * <li>Maintaining the list of suspects and notifying listeners of changes.</li>
 * </ul>
 * <p/>
 * This class is thread-safe.
 */
public abstract class StronglyCompleteFailureDetector implements FailureDetector, MessageListener {
    private static final Logger LOG = Logger.getLogger(StronglyCompleteFailureDetector.class);

    /**
     * How often is heartbeat broadcast in ms.
     */
    public static final int HEARTBEAT_PERIOD_MS = 1000;

    /**
     * 1 thread per process timeout scheduler + a couple of additional ones for consensus, heartbeat sending, ...
     * Only increase this when the number of processes handled by the system should be greater than 10.
     */
    private static final int THREADS_PER_DETECTOR = 20;

    /**
     * Process to which this instance of failure detector belongs.
     */
    protected final Process process;

    /**
     * Thread-safe map of timeout strategies per process.
     */
    private final ConcurrentMap<Integer, TimeoutStrategy> timeoutStrategies = new ConcurrentHashMap<Integer, TimeoutStrategy>();

    /**
     * Thread-safe map of scheduled suspicions per process.
     */
    private final ConcurrentMap<Integer, Future<?>> scheduledSuspicions = new ConcurrentHashMap<Integer, Future<?>>();

    /**
     * Thread-safe set of currently suspected processes. The value is irrelevant (always true). This is just because
     * there is no better way of implementing a concurrent set with atomic operations in java.
     */
    private final ConcurrentMap<Integer, Boolean> suspects = new ConcurrentHashMap<Integer, Boolean>();

    /**
     * Thread-safe list of suspect listeners.
     */
    private final Set<WeakReference<SuspectListener>> suspectListeners = new CopyOnWriteArraySet<WeakReference<SuspectListener>>();

    /**
     * Executor for scheduled tasks with a fixed thread pool. Tasks that can't be accepted will be aborted (corresponding exception will be thrown).
     */
    protected final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(THREADS_PER_DETECTOR, new ThreadPoolExecutor.AbortPolicy());

    /**
     * Constructor.
     *
     * @param process to which this failure detector belongs.
     */
    public StronglyCompleteFailureDetector(Process process) {
        this.process = process;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        process.addMessageListener(this);
        initTimeoutStrategiesAndScheduleFirstSuspicions();
        scheduleHeartbeatBroadcast();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        executor.shutdownNow();
    }

    /**
     * Initialize the per-process timeout strategies and schedule first suspicions.
     */
    private void initTimeoutStrategiesAndScheduleFirstSuspicions() {
        for (int pid = 1; pid <= process.getNumberOfProcesses(); pid++) {
            if (pid != process.getProcessId()) {
                timeoutStrategies.put(pid, newTimeoutStrategy());
                scheduleNewSuspicion(pid);
            }
        }
    }

    /**
     * Get an instance of the timeout strategy for a new process (determined by the subclass).
     *
     * @return an instance of the strategy for a new process. This should be a new instance every time, unless the
     *         strategy is a singleton.
     */
    protected abstract TimeoutStrategy newTimeoutStrategy();

    /**
     * {@inheritDoc}
     * <p/>
     * Let's the per-process timeout strategy know that a process has sent a message and delegate other potential
     * handling of the message to the subclass.
     */
    @Override
    public final void receive(final TimestampedProcessToProcessMessage m) {
        timeoutStrategies.get(m.getSource()).messageReceived(m);
        doReceive(m);
    }

    /**
     * Allow subclasses to handle received messages.
     *
     * @param m message.
     */
    protected abstract void doReceive(final TimestampedProcessToProcessMessage m);

    /**
     * Schedule regular heartbeat broadcast.
     */
    private void scheduleHeartbeatBroadcast() {
        Runnable sendHeartBeatTask = new Runnable() {
            @Override
            public void run() {
                process.send(new HeartbeatMessage(process.getProcessId()));
            }
        };
        executor.scheduleAtFixedRate(sendHeartBeatTask, 0, HEARTBEAT_PERIOD_MS, MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSuspectListener(SuspectListener suspectListener) {
        suspectListeners.add(new WeakReference<SuspectListener>(suspectListener));
    }

    /**
     * Schedule a new timeout task for the given process.
     * Cancel and replace the existing timeout task (if exists).
     *
     * @param pid for which to schedule timeout.
     */
    protected void scheduleNewSuspicion(final int pid) {
        FutureTask<?> scheduledSuspicion = new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                addToSuspects(pid);
                return null;  //no result expected
            }
        });

        executor.schedule(scheduledSuspicion, timeoutStrategies.get(pid).getNextTimeout(), MILLISECONDS);

        Future<?> previousSuspicion = scheduledSuspicions.put(pid, scheduledSuspicion);

        if (previousSuspicion != null) {
            previousSuspicion.cancel(true);
        }
    }

    /**
     * Add a process to the list of suspects and notify listeners.
     *
     * @param pid ID of the new suspect.
     */
    protected final void addToSuspects(int pid) {
        if (suspects.putIfAbsent(pid, true) == null) {
            notifySuspectListeners();
        }
    }

    /**
     * Remove a process from the list of suspects, if present, and notify listeners.
     *
     * @param pid ID of the no longer suspected process.
     */
    protected final void removeFromSuspects(int pid) {
        if (suspects.remove(pid, true)) {
            notifySuspectListeners();
        }
    }

    /**
     * Notify suspect listeners about an update to the list of suspects.
     * The list of suspects is provided as a read-only copy.
     */
    private void notifySuspectListeners() {
        LOG.info(process.getName() + " updated suspects: " + Arrays.toString(getSuspects().toArray()));

        for (WeakReference<SuspectListener> reference : suspectListeners) {
            SuspectListener suspectListener = reference.get();
            if (suspectListener != null) {
                suspectListener.suspectsUpdated(getSuspects());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuspect(int process) {
        return suspects.containsKey(process);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Integer> getSuspects() {
        return Collections.unmodifiableSet(new HashSet<Integer>(suspects.keySet()));
    }
}
