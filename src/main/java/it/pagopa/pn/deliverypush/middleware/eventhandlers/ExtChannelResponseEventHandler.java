package it.pagopa.pn.deliverypush.middleware.eventhandlers;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
public class ExtChannelResponseEventHandler extends AbstractEventHandler<PnExtChnProgressStatusEvent> {

    private final ActionsPool actionsPool;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public ExtChannelResponseEventHandler(ActionsPool actionsPool , PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super( PnExtChnProgressStatusEvent.class );
        this.actionsPool = actionsPool;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    @Override
    public void handleEvent(PnExtChnProgressStatusEvent evt ) {
        StandardEventHeader header = evt.getHeader();
        log.info( "EXT_CHANNEL RESPONSE iun={} eventId={}", header.getIun(), header.getEventId() );

        String sendActionId = evt.getPayload().getRequestCorrelationId();

        Optional<Action> sendAction = actionsPool.loadActionById( sendActionId );

        if( sendAction.isPresent() ) {
            Action extChResponseAction = sendAction.get().toBuilder()
                    .type( ActionType.RECEIVE_PEC )
                    .notBefore( header.getCreatedAt().plus( pnDeliveryPushConfigs.getTimeParams().getTimeBetweenExtChReceptionAndMessageProcessed() ) )
                    .responseStatus( evt.getPayload().getStatusCode() )
                    .build();

            actionsPool.scheduleFutureAction( extChResponseAction.toBuilder()
                    .actionId( ActionType.RECEIVE_PEC.buildActionId( extChResponseAction ))
                    .build()
            );
        }
        else {
            throw new PnInternalException("Action with id " + sendActionId + " not found");
        }

    }

}
