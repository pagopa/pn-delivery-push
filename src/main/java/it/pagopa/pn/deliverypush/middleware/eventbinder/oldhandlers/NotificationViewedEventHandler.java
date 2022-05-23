package it.pagopa.pn.deliverypush.middleware.eventbinder.oldhandlers;
/*

import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationViewedEventHandler extends AbstractEventHandler<PnDeliveryNotificationViewedEvent> {

    private final ActionsPool actionsPool;

    public NotificationViewedEventHandler( ActionsPool actionsPool ) {
        super( PnDeliveryNotificationViewedEvent.class );
        this.actionsPool = actionsPool;
    }

    @Override
    public void handleEvent(PnDeliveryNotificationViewedEvent evt ) {

        StandardEventHeader header = evt.getHeader();
        log.info( "NOTIFICATION VIEWED  iun={} eventId={}", header.getIun(), header.getEventId() );

        Action notificationViewed = Action.builder()
                .iun(header.getIun() )
                .recipientIndex( evt.getPayload().getRecipientIndex() )
                .actionId( header.getEventId() )
                .type( ActionType.NOTIFICATION_VIEWED )
                .notBefore( header.getCreatedAt() )
                .build();
        
        actionsPool.scheduleFutureAction( notificationViewed );

    }

}
*/
