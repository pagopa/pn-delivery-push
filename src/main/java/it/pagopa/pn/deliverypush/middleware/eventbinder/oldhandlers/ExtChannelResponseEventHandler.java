package it.pagopa.pn.deliverypush.middleware.eventbinder.oldhandlers;

/*
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatusEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            PhysicalAddress newPhysicalAddress = null;
            
            it.pagopa.pn.api.dto.notification.address.PhysicalAddress newPhysicalAddressExt = evt.getPayload().getNewPhysicalAddress();
            if( newPhysicalAddressExt != null){
                newPhysicalAddress = PhysicalAddress.builder()
                        .foreignState(newPhysicalAddressExt.getForeignState())
                        .at(newPhysicalAddressExt.getAt())
                        .addressDetails(newPhysicalAddressExt.getAddressDetails())
                        .zip(newPhysicalAddressExt.getZip())
                        .municipality(newPhysicalAddressExt.getMunicipality())
                        .province(newPhysicalAddressExt.getProvince())
                        .address(newPhysicalAddressExt.getAddress())
                        .build();
            }

            ActionType receiveActionType = sendToReceiveActionType( sendAction.get() );
            Action extChResponseAction = sendAction.get().toBuilder()
                    .type( receiveActionType )
                    .notBefore( header.getCreatedAt().plus( pnDeliveryPushConfigs.getTimeParams().getTimeBetweenExtChReceptionAndMessageProcessed() ) )
                    .responseStatus( evt.getPayload().getStatusCode() )
                    .newPhysicalAddress(newPhysicalAddress)
                    .attachmentKeys( evt.getPayload().getAttachmentKeys() )
                    .build();

            actionsPool.scheduleFutureAction( extChResponseAction.toBuilder()
                    .actionId( receiveActionType.buildActionId( extChResponseAction ))
                    .build()
            );
        }
        else {
            throw new PnInternalException("Action with id " + sendActionId + " not found");
        }

    }

    private ActionType sendToReceiveActionType( Action sendAction) {

        ActionType result;
        switch ( sendAction.getType() ) {
            case SEND_PEC: result = ActionType.RECEIVE_PEC; break;
            case SEND_PAPER: result = ActionType.RECEIVE_PAPER; break;
            case PEC_FAIL_SEND_PAPER: result = ActionType.PEC_FAIL_RECEIVE_PAPER; break;
            default:
                throw new PnInternalException( sendAction.getType() + " is not a send");
        }
        return result;

    }

}

 */
