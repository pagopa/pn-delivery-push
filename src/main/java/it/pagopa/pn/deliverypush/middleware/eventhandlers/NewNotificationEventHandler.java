package it.pagopa.pn.deliverypush.middleware.eventhandlers;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class NewNotificationEventHandler extends AbstractEventHandler<PnDeliveryNewNotificationEvent> {

    private final ActionsPool actionsPool;

    public NewNotificationEventHandler( ActionsPool actionsPool ) {
        super( PnDeliveryNewNotificationEvent.class );
        this.actionsPool = actionsPool;
    }

    @Override
    public void handleEvent(PnDeliveryNewNotificationEvent evt ) {
        StandardEventHeader header = evt.getHeader();
        log.info( "NEW NOTIFICATION iun={} eventId={}", header.getIun(), header.getEventId() );

        Action senderAck = Action.builder()
                .iun(header.getIun() )
                .actionId( header.getEventId() )
                .type( ActionType.SENDER_ACK )
                .notBefore( header.getCreatedAt().plus( 5, ChronoUnit.SECONDS ))
                .build();
        actionsPool.scheduleFutureAction( senderAck );
    }

}
