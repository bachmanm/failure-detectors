package listener;

import java.util.Set;

/**
 * Interface for listeners that wish to be notified about the set of suspects being updated.
 */
public interface SuspectListener {

    /**
     * Handle an updated listener set.
     *
     * @param suspects a immutable copy/snapshot of the current set of suspects.
     */
    void suspectsUpdated(Set<Integer> suspects);
}
