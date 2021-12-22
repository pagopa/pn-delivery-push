package it.pagopa.pn.deliverypush.middleware.eventhandlers;

import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NewNotificationEventHandler extends AbstractEventHandler<PnDeliveryNewNotificationEvent> {

    private final ActionsPool actionsPool;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public NewNotificationEventHandler( ActionsPool actionsPool , PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super( PnDeliveryNewNotificationEvent.class );
        this.actionsPool = actionsPool;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    @Override
    public void handleEvent(PnDeliveryNewNotificationEvent evt ) {
        StandardEventHeader header = evt.getHeader();
        log.info("NEW NOTIFICATION iun={} eventId={}", header.getIun(), header.getEventId());

        //TODO Perchè viene schedulata un azione futura??

        Action senderAck = Action.builder()
                .iun(header.getIun())
                .actionId(header.getEventId())
                .type(ActionType.SENDER_ACK)
                //ritardo tra ricezione messaggio new notifica e sua ricezione
                .notBefore(header.getCreatedAt().plus(pnDeliveryPushConfigs.getTimeParams().getIntervalBetweenNotificationAndMessageReceived()))
                .build();
        actionsPool.scheduleFutureAction(senderAck);
    }

}
