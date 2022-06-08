package it.pagopa.pn.deliverypush.abstractions.webhookspool;

public interface WebhooksPool {
     void scheduleFutureAction(WebhookAction action, WebhookEventType webhookEventType);
}
