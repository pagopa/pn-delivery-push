package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;

import java.util.Optional;

public interface ActionService {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.action";

    void addOnlyActionIfAbsent(Action action);

    Optional<Action> getActionById(String actionId );

    void unSchedule(Action action, String timeSlot);
}
