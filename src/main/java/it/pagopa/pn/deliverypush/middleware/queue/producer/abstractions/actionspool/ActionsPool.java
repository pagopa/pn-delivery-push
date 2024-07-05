package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;

import java.time.Instant;
import java.util.Optional;

public interface ActionsPool {
    void scheduleFutureAction(Action action);
    void startActionOrScheduleFutureAction(Action action );
    void addOnlyAction(Action action);
    void unscheduleFutureAction( String actionId );
    Optional<Action> loadActionById( String sendActionId );
    boolean isPerformanceImprovementEnabled(Instant notBefore);
}
