package it.pagopa.pn.deliverypush.middleware.dao.actiondao;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;

public interface ActionDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.action-dao";
    void addOnlyActionIfAbsent(Action action);
}
