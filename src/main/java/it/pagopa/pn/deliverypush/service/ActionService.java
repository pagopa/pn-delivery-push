package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;

public interface ActionService {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.action";

    void addOnlyActionIfAbsent(Action action);
    void unSchedule(String actionId);
}
