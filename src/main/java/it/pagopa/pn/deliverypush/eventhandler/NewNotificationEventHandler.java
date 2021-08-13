package it.pagopa.pn.deliverypush.eventhandler;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.NewNotificationEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class NewNotificationEventHandler implements EventHandler<NewNotificationEvent> {

    private final MomProducer<PnExtChnPecEvent> sendPec;

    public NewNotificationEventHandler(MomProducer<PnExtChnPecEvent> sendPec) {
        this.sendPec = sendPec;
    }

    @Override
    public void handle( NewNotificationEvent evt ) {
        StandardEventHeader header = evt.getHeader();
        log.info( "Event: {} new notification received with iun {}",
                                                      header.getEventId(), header.getIun() );

        PnExtChnPecEvent extChRequest = PnExtChnPecEvent.builder()
                .header( StandardEventHeader.builder()
                        .iun(header.getIun())
                        .publisher( "PN_DELIVERY_PUSH" )
                        .eventType( EventType.SEND_PEC_REQUEST )
                        .eventId(header.getIun() + "_pec_rec1_address1_retry1")
                        .createdAt( Instant.now() )
                    .build()
                )
                .build();
        sendPec.push( extChRequest );
    }

    @Override
    public EventType getEventType() {
        return EventType.NEW_NOTIFICATION;
    }
}
