package it.pagopa.pn.deliverypush.abstractions.actionspool;

import java.util.Optional;

public interface ActionsPool {

    void scheduleFutureAction( Action action );

    Optional<Action> loadActionById( String sendActionId );

    void pollForFutureActions();
}
