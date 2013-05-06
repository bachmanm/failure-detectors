package process;

import broker.Broker;
import broker.FailureInjector;
import broker.FixedDelayBroker;
import broker.GaussianDelayBroker;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import process.base.*;
import process.base.Process;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Util class with convenience methods for process smoke tests.
 */
public abstract class ProcessSmokeTest<T extends process.base.Process> {
    private static final Logger LOG = Logger.getLogger(ProcessSmokeTest.class);

    private Broker broker;
    private FailureInjector failureInjector;
    protected final List<T> processes = new ArrayList<T>();

    @Before
    public void setUp() {
        broker = createBroker();
        failureInjector = new FailureInjector();
    }

    protected abstract Broker createBroker();

    @After
    public void tearDown() {
        for (Process process : processes) {
            process.stop();
        }
        broker.shutdown();
    }

    protected void launchProcess(int processId, int numberOfProcesses) {
        T process = createProcess("Process" + processId, processId, numberOfProcesses);
        processes.add(process);
        process.start();
    }

    protected void launchProcesses(int number) {
        for (int i = 1; i <= number; i++) {
            launchProcess(i, number);
        }
    }

    protected abstract T createProcess(String processName, int processId, int numberOfProcesses);

    public void killProcesses(int... processIds) throws IOException {
        for (int pid : processIds) {
            failureInjector.killProcess(pid);
        }
    }

    public void resurrectProcesses(int... processIds) throws IOException {
        for (int pid : processIds) {
            failureInjector.restoreProcess(pid);
        }
    }
}
