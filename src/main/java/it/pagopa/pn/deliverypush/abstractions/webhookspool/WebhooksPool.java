package it.pagopa.pn.deliverypush.abstractions.webhookspool;

import java.util.Optional;

public interface WebhooksPool {
     void addWebhookAction(WebhookAction action, WebhookEventType webhookEventType);
}
