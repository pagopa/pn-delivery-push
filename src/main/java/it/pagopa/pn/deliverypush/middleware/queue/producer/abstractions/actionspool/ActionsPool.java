package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;


public interface ActionsPool {
    void addOnlyAction(Action action);
}
