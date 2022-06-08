package it.pagopa.pn.deliverypush.abstractions.webhookspool.impl;

import it.pagopa.pn.api.dto.events.IEventType;

public enum WebhookActionEventType implements IEventType {
    WEBHOOK_ACTION_GENERIC( WebhookEvent.class );

    private final Class<?> eventClass;

    WebhookActionEventType(Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<?> getEventJavaClass() {
        return eventClass;
    }

}
