package it.pagopa.pn.deliverypush.eventhandler;

import it.pagopa.pn.api.dto.events.EventType;

public interface EventHandler<E> {

    void handle( E evt );

    EventType getEventType();
}
