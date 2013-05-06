# Failure Detectors

a.k.a. Agreement in Asynchronous Distributed Systems

## Introduction

Asynchronous distributed systems pose special challenges to software design and development due to the lack of any 
timing assumptions under this model. One of the most important problems is determining whether a participating process
is still *correct* (but slow) or has *crashed*. Fischer, Lynch, and Paterson (FLP) proved that it is indeed impossible 
to predict failures perfectly in a fully asynchronous system without putting further assumptions in place. Thus, it is 
impossible to solve problems like consensus and atomic broadcast deterministically even for a single process failure.

### Failure Detectors

Chandra and Toueg address this problem in their seminal papers, introducing unreliable *failure detectors*.  
They work around the FLP limitation by allowing processes to _suspect_ that others have failed, usually based on liveness 
criteria, thus effectively bringing them back into synchrony. They introduce two main properties of such failure detectors: 
*completeness* and *accuracy*.

### Completeness and Accuracy

*Completeness* guarantees that all failed processes are eventually permanently suspected by a correct process.  
Borrowing from statistics terminology, completeness gives us a measure of type II error. Completeness is further 
sub-divided into _strong completeness_, under which all failed processes are eventually suspected by _all_ correct processes, 
and _weak completeness_, under which all failed processes are eventually suspected by _some_ correct process.

_Accuracy_, on the other hand, ensures that a correct process is not suspected by any correct process. 
Continuing with the statistics analogy, accuracy gives us an indication of whether a type I error is made by the failure detector. 
_Strong accuracy_ ensures that _all_ correct processes are never suspected by any correct process, whilst _weak accuracy_ 
ensures that _at least one_ correct process is never suspected by any correct process. By further relaxing these
perpetual accuracy properties, two additional versions arise: _eventual strong accuracy_, under which strong accuracy is
guaranteed after some time in the future, and _eventual weak accuracy_, under which weak accuracy is guaranteed after a future time.

Classes of Failure Detectors:

|Detector           |Completeness   |Accuracy           |
|--------           |------------   |--------
|Perfect            |Strong         |Strong             |
|Eventually Perfect |Strong         |Eventually Strong  |
|Strong             |Strong         |Weak               |
|Eventually Strong  |Strong         |Eventually Weak    |
|Weak				|Weak			|Weak               |
|Eventually Weak	|Weak			|Eventually Weak    |
|             		|Weak      	    |Strong             |
|  				    |Weak      	    |Eventually Strong  |

Combining the properties, eight classes of failure detectors can be identified. Two important applications of failure
detectors are leader election and consensus in asynchronous distributed systems. The first four classes of failure detectors,
a leader election algorithm, and two types of consensus algorithms have been designed, implemented, and tested. They are
available in this repository.

## Implementation

### Failure Detectors

#### Design

All failure detectors have been implemented using periodic _heartbeat_ messages, broadcast by a process to signal that
it is alive/correct. When a process hasn't sent a heartbeat message within a _timeout period_, the failure detector
adds that process to a list of suspects. A failed process does not send heartbeat and will, therefore, eventually
become suspected by every correct process running the failure detector. This way, strong completeness is achieved.

Sending heartbeats and maintaining suspects is the responsibility of an abstract class called `StronglyCompleteFailureDetector`.
The responsibility of determining the timeout periods is delegated to the subclasses via the `TimeoutStrategy` interface.
As discussed later, different levels of accuracy can be achieved by choosing the right timeout strategy.
Additionally, the subclasses (i.e. concrete failure detector implementations) are responsible for dealing with received
heartbeat (and other) messages.

![Interfaces and Base Classes for Failure Detector Implementations](https://github.com/bachmanm/failure-detectors/raw/master/report/diagram1.png "Interfaces and Base Classes for Failure Detector Implementations")

#### Implementation

As apparent from the discussion thus far, a number of concurrent tasks are handled by the system.
These are implemented in concurrent threads, which, naturally, makes the system multi-threaded. Special care has,
thus, been taken to accommodate this fact. The system uses a `ScheduledThreadPoolExecutor` to concurrently execute
(potentially scheduled) tasks using a fixed thread pool.

_Heartbeats_ are the first example of such a scheduled task. Upon initialisation, the failure detector schedules a
recurring task that broadcasts heartbeat every `HEARTBEAT_PERIOD_MS` milliseconds (by default set to 1000).

```java
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
```

The heartbeat message carries a payload containing the _timestamp_ of the message creation, so that the receiving
process can easily evaluate the _delay_, with which the message arrived. This assumes roughly synchronized clocks, of course.
In this single-JVM simulation, this is a valid assumption. If this were a real distributed system, the delay would have
to be evaluated by other means, such as the receiving process measuring time between messages.

`StronglyCompleteFailureDetector` is also responsible for managing _suspected_ processes. Upon initialisation,
each process in the ensemble is _scheduled_ for suspicion at some future time, referred to as the _timeout period_.
This is achieved using another scheduled task, which will add that process to the list of suspects when executed.
Upon scheduling a new suspicion for a process, the previously scheduled one is cancelled.

```java
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
```

After initialization, the concrete implementations of failure detectors are responsible for scheduling new suspicions
for processes, as they see fit. `TimeoutStrategy` is responsible for the actual value of the timeout.

Every time the set of suspected processes changes, the `StronglyCompleteFailureDetector` notifies any component that 
has registered to listen for such updates. Components can do so by implementing the `SuspectListener` interface and
explicitly registering themselves on the appropriate detector.

### Perfect Failure Detector

With the basic plumbing in place, a few decisions had to be made in order to design and implement a perfect failure detector.

#### Design

A perfect failure detector assumes a synchronous environment and thus an upper bound on the message delay. If a process 
hasn't received a heartbeat message within a timeout period defined by this upper bound, the sender has crashed.
To account for scheduling and other overheads, this timeout period is set to T = Delta + 2*d, where Delta is the heartbeat 
period and d is the average message delay. Since processes never recover from crashes in the assumed model, 
once the process is suspected, it will never be removed from the suspect set. Therefore, all future messages from a 
suspected process are ignored.

#### Implementation

The perfect failure detector is implemented in the `PerfectFailureDetector` class. A very simple `UpperBoundTimeoutStrategy`
has been implemented for the perfect failure detector. This strategy always returns a `DEFAULT_TIMEOUT_PERIOD`, which is 
defined as  `public static final int DEFAULT_TIMEOUT_PERIOD = HEARTBEAT_PERIOD_MS + 2 * DELAY`. 
Because it does not maintain any per-process state, it is a class designed to be used as a singleton.

Upon receiving a message from a process, the perfect failure detector schedules a new suspicion for that process without 
checking the type of the message. This means application messages are also treated as heartbeat (on the receiving side). 
However, if a process is already suspected, any messages are simply ignored, as suggested in the previous section.  

```java
/**
 * {@inheritDoc}
 * <p/>
 * Schedules a new timeout task for the sender,
 * unless the process is already suspect, in which case the message is ignored.
 */
@Override
public void doReceive(final TimestampedProcessToProcessMessage m) {
    if (!isSuspect(m.getSource())) {
        scheduleNewSuspicion(m.getSource());
    }
}
```

#### Test

The perfect failure detector is automatically unit-tested in `PerfectFailureDetectorTest`. 
The following scenarios are considered: 
	* Correct number of heartbeat messages are sent at different points in time
	* No processes are initially suspected
	* No processes are suspected before the first timeout period elapses
	* Processes not sending heartbeats are suspected after a timeout period
	* Processes sending heartbeats are not suspected after a timeout period
	* Suspected process stays suspected, even if a message arrives from it
	* Listeners are correctly notified of suspect changes

### Eventually Perfect Failure Detector

#### Design

An eventually perfect failure detector assumes an asynchronous environment that will eventually become synchronous.
Thus, there is no fixed upper bound on the message delay and the detector may temporarily falsely suspect a correct process.
For example, processes having a Gaussian message delay will sometimes take longer to send a heartbeat and will thus be
added to the suspects list. To eventually become perfect, two _adaptive_ timeout strategies that predict the upper bound
for the next timeout for a given process have been considered.

Using the ''maximum delay'' strategy, the detector keeps track of the maximum delay seen thus far for a given process
and uses that value as the next timeout for that process. The advantage of such strategy is that eventually,
one delay will be the maximum delay ever seen for the process and that process will therefore never be suspect again,
making the failure detector eventually perfect. The drawback of such strategy is that as time progresses and maximum
recorded delays increase, the system will become slower in suspecting crashed processes.

To overcome the performance problem of the strategy mentioned above, an ''average delay'' strategy has been considered.
Such strategy keeps a record of the average message delay seen so far for a given process and uses that as the next
timeout period for that process. In theory, this means that at any point in time, one cannot be sure that a
longer-than-the-average delay will not occur in the future. However, since the definition of eventually perfect failure
detector does not specify when it must become perfect, all algorithms designed for eventually perfect failure detectors
should work with this strategy.


![Timeout Strategies. Please note that instances of these objects are used per process, i.e. each process gets its own instance that is then maintained independently.](https://github.com/bachmanm/failure-detectors/raw/master/report/diagram2.png "Timeout Strategies. Please note that instances of these objects are used per process, i.e. each process gets its own instance that is then maintained independently.")

Since processes never recover from crashes but may be falsely suspected, a process is removed from the set of suspected
processes if a message from that process arrives at any point in time.

#### Implementation

The eventually perfect failure detector is implemented in the `EventuallyPerfectFailureDetector` class.

`AdaptiveMaxTimeoutStrategy` and `AdaptiveAverageTimeoutStrategy` have been implemented for the eventually perfect failure detector.
These strategies start out with a default delay for each process and adaptively adjust this delay as messages from the processes arrive.

Upon receiving a message from a process, the eventually perfect failure detector schedules a new suspicion for that
process similarly to the perfect failure detector. There is a difference, however, in the handling of suspected processes
in the sense that a process from which a message was received is removed from suspects.

```java
/**
 * {@inheritDoc}
 *
 * Schedules a new suspicion for the sender and removes the process from the list of suspects if present.
 */
@Override
public void doReceive(final TimestampedProcessToProcessMessage m) {
    scheduleNewSuspicion(m.getSource());
    removeFromSuspects(m.getSource());
}
```

#### Test

The eventually perfect failure detector is automatically unit-tested in `EventuallyPerfectFailureDetectorTest`.
The test scenarios are exactly the same as in the case of `PerfectFailureDetectorTest` with the following exception:
	* Suspected process _becomes un-suspected, when a message arrives from it_

### Eventual Leader Election

An eventual leader election algorithm has been designed and implemented, whereby all correct processes eventually agree
on the same correct process to be the leader. It is a correct process with the highest process ID.

#### Design

The eventually perfect failure detector has been extended, gaining the eventual leader election capability.
Since the eventually perfect failure detector eventually suspects all crashed processes and does not suspect any correct
ones, each of the correct processes in the ensemble eventually share the same view on the state of the system and elect
the same leader. The leader election is triggered by every update to the set of suspects. For this reason, the eventual
leader elector implements the `SuspectListener` interface.

#### Implementation

The eventual leader elector is implemented in the `LeaderElectingEventuallyPerfectFailureDetector` class.

Whenever the suspects set is updated, the `electNewLeader` method is triggered. The process with highest ID that isn't
suspected becomes the new leader.

```java
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
```

#### Test

The eventual leader elector is automatically unit-tested in `LeaderElectingEventuallyPerfectFailureDetectorTest`.
The following test scenarios are executed:
	* The first leader is elected right after initialisation and it is the process with the highest process ID
	* When the current leader becomes suspected, a new leader is elected
	* When all other processes become suspected, the process elects itself leader
	* When a process with higher ID than the current leader becomes un-suspected, it is elected leader

### Consensus

In distributed systems, it is often important to bring processes into agreement, as in the case of committing a
transaction to a distributed database. Two different implementations of consensus capable failure detectors have been
created, using two different versions of the rotating coordinators algorithm. Before taking a deeper look at each implementation,
let's have a look at their design considerations, as they are identical for both.

#### Design

The two aforementioned consensus-reaching algorithms are designed to be used with a strong and eventually strong failure
detectors. These can be emulated with perfect and eventually perfect failure detectors, respectively. Therefore, their
implementations are extended for the purpose of this exercise.

Both rotating coordinators algorithms require some blocking operations. An example is the `collect` operation, which
waits for a message from a process or for that process to become suspected. To accommodate this requirement,
the consensus-reaching operations are treated as another example of tasks that can be submitted to the `executor`.
Since they run in separate threads, they ''live their own life'' and their blocking does not affect the rest of the system.
These blocking operations can be unblocked by one of the two things: a message reception and a suspect set update.
Hence, the classes responsible for achieving consensus implement both `MessageListener` and `SuspectListener` interfaces.
Message reception and/or suspect set update is triggered from a different thread; this notifies the blocked threads and
allows them to re-evaluate their blocking condition.

The lack of synchrony in an asynchronous system presents the following challenge: although the consensus-reaching
algorithms have a notion of ''rounds'', different processes can be in very different rounds at any point in time.
Therefore, in order for any messages not to be lost, every consensus-related message that arrives at a process is stored,
although it might be used in one of the future rounds.

A hierarchy of classes has been designed to cleanly represent unknown values, collected values, and values not collected
due to process suspicions.

![Value Class Hierarchy](https://github.com/bachmanm/failure-detectors/raw/master/report/diagram5.png "Value Class Hierarchy")

#### Implementation of Consensus with Strong Failure Detector

The consensus algorithm designed for strong failure detectors is implemented in the `StrongConsensus` class.
It can then be used in a process that uses a `StrongFailureDetector`, a simple extension to the perfect failure detector implementation.
The key part of the algorithm is presented here:

```java
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
```
```java
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
```

```java
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
```

#### Implementation of Consensus with Eventually Strong Failure Detector

The `EventuallyStrongConcensus` class implements the consensus algorithm used for eventually strong failure detectors.
It can then be used in a process in conjunction with `EventuallyStrongFailureDetector`, a simple extension to the eventually
perfect failure detector implementation. In this case, a more intricate algorithm is needed to arrive at consensus.
Here, the eventually strong property of the failure detector requires an upper bound of N/3 failed processes.
The following code succinctly summarises this process. The reader is kindly referred to the actual code for more detail.

```java
/**
 * {@inheritDoc}
 * <p/>
 * Starts the consensus process.
 */
@Override
public synchronized String call() throws Exception {
    while (true) {
        initializeNewRound();
        everyoneSendProposalToCoordinator();
        coordinatorCollectProposalsAndBroadcastOutcome();
        everyoneCollectOutcomeFromCoordinator();
        if (decideAndTerminate()) return currentProposal;
    }
}
```

#### Test

The consensus-reaching components aren't fully unit-tested. Instead, ''smoke-tests'' and demos have been written.
Demos are `@Ignored` from the normal test-compile lifecycle.

## References
  * Tushar Deepak Chandra, Sam Toueg, "Unreliable Failure Detectors for Reliable Distributed Systems.", Journal of the ACM, 43(2):225-267, 1996.
  * Nancy Lynch, 1996. Distributed Algorithms, Morgan Kaufman Publishers.
  * Ajay D. Kshemkalyani, Mukesh Singhal, 2008. Distributed Computing: Principles, Algorithms, and Systems, Cambridge University Press.
  * Michael J. Fischer, Nancy A. Lynch, Michael S. Paterson, "Impossibility of Distributed Consensus with One Faulty Process", Journal of the ACM, Vol. 32, No. 2, April 1985, pp. 374-382.

## Getting the code

 In case you don't have the code already, you can obtain it freely from https://github.com/bachmanm/failure-detectors.git

## Compiling the Code

 [Maven](http://maven.apache.org/) is used as the build and dependency management tool. It is a required pre-requisite
 to compile the code. It has been tested with version 3.0.3.

 In the root of the project (where the `pom.xml` file is located), run `mvn clean compile`. This will compile the code.
 Run `mvn clean package` to compile and run automated tests.

## Running the Code

 Have a look at any of the `*Demo` classes, remove the `@Ignore` at the top and run them. Have fun!