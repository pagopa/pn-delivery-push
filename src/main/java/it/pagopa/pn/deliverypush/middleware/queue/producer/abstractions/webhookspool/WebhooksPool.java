package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool;

public interface WebhooksPool {
     void scheduleFutureAction(WebhookAction action);
}
