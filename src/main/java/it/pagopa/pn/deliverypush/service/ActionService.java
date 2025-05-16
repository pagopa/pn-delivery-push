package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;

public interface ActionService {

    void addOnlyActionIfAbsent(Action action);
}
