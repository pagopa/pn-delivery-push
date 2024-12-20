package it.pagopa.pn.deliverypush.middleware.dao.actiondao;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;

import java.util.List;
import java.util.Optional;

public interface ActionDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.action-dao";

    void addAction( Action action, String timeSlot );

    void addOnlyAction(Action action);

    void addOnlyActionIfAbsent(Action action);
    void addActionAndFutureActionIfAbsent(Action action, String timeSlot);
        
    Optional<Action> getActionById( String actionId );

    List<Action> findActionsByTimeSlot( String timeSlot );

    void unScheduleFutureAction(Action action, String timeSlot );
}
