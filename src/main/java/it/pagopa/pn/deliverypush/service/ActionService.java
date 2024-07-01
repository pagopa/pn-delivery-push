package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;

import java.util.List;
import java.util.Optional;

public interface ActionService {
    void addActionAndFutureActionIfAbsent(Action action, String timeSlot);

    void addOnlyActionIfAbsent(Action action);

    void addOnlyAction(Action action);

    Optional<Action> getActionById(String actionId );

    List<Action> findActionsByTimeSlot(String timeSlot );

    void unSchedule( Action action, String timeSlot );
}
