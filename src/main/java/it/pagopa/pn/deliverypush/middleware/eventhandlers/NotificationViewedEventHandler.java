package it.pagopa.pn.deliverypush.middleware.eventhandlers;

import org.springframework.stereotype.Service;

import it.pagopa.pn.api.dto.events.PnDeliveryNotificationAcknowledgementEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationViewedEventHandler extends AbstractEventHandler<PnDeliveryNotificationAcknowledgementEvent> {

    private final ActionsPool actionsPool;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public NotificationViewedEventHandler( ActionsPool actionsPool , PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super( PnDeliveryNotificationAcknowledgementEvent.class );
        this.actionsPool = actionsPool;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    @Override
    public void handleEvent(PnDeliveryNotificationAcknowledgementEvent evt ) {
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
