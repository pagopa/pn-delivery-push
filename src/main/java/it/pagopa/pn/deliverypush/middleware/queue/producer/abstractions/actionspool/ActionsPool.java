package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;

import java.util.Optional;

public interface ActionsPool {

    void scheduleFutureAction( Action action );

    Optional<Action> loadActionById( String sendActionId );
}
