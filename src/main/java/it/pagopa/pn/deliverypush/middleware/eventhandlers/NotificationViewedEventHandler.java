package it.pagopa.pn.deliverypush.middleware.eventhandlers;

import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import org.springframework.stereotype.Service;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import lombok.extern.slf4j.Slf4j;

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
