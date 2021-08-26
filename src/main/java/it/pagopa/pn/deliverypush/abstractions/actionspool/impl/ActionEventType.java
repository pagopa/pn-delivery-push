package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.IEventType;

public enum ActionEventType implements IEventType {
    ACTION_GENERIC( ActionEvent.class );

    private final Class<?> eventClass;

    ActionEventType( Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<?> getEventJavaClass() {
        return eventClass;
    }

}
