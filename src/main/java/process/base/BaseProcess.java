package process.base;

/**
 * Abstract base-class for all processes.
 */
public abstract class BaseProcess implements Process {

    private final String name;
    private final int processId;
    private final int numberOfProcesses;

    /**
     * Create a new process.
     *
     * @param name              process name.
     * @param processId         process ID.
     * @param numberOfProcesses total number of processes in the ensemble.
     */
    public BaseProcess(String name, int processId, int numberOfProcesses) {
        this.name = name;
        this.processId = processId;
        this.numberOfProcesses = numberOfProcesses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessId() {
        return processId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfProcesses() {
        return numberOfProcesses;
    }
}
