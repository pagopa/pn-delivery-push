package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool;

public enum WebhookEventType {
    PURGE_STREAM_OLDER_THAN(),

    PURGE_STREAM(),

    REGISTER_EVENT();
}
