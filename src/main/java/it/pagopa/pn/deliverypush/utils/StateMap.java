package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
class StateMap {
    private final static boolean FOR_MULTI_RECIPIENT = true; // il multi-destinatario comprende casi AGGIUNTIVI al singolo destinatario
    private final static boolean SINGLE_RECIPINET = false;

    private final Map<MapKey, MapValue> mappings = new HashMap<>();
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public StateMap(PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        
        // Received state
        this.fromState(NotificationStatusInt.IN_VALIDATION)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_ACCEPTED, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_REFUSED, NotificationStatusInt.REFUSED, SINGLE_RECIPINET)
        ;
                
        this.fromState(NotificationStatusInt.ACCEPTED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.UNREACHABLE, SINGLE_RECIPINET) //Casista tutti gli indirizzi digitali e analogici non sono presenti
                .withTimelineGoToState(TimelineElementCategoryInt.NOT_HANDLED, NotificationStatusInt.CANCELLED, SINGLE_RECIPINET)
        ;

        // Delivering state
        this.fromState(NotificationStatusInt.DELIVERING)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Fallito workflow analogico, ci sarà l'elemento di timeline Completely unreachable che porta allo stato UNREACHABLE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Per le notifiche multi recipient potrebbe esserci il REFINEMENT anche in fase di DELIVERING (perchè la notifica potrebbe essere stata consegnata per un destinatario ma non per i restanti)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Con i MultiDestinatari potrebbe essere schedulato l'analog workflow anche in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Con i MultiDestinatari potrebbe essere inviato il messaggio di cortesia in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Multi Destinatari
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
        
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.UNREACHABLE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOT_HANDLED, NotificationStatusInt.CANCELLED, SINGLE_RECIPINET)
        ;

        // Delivered state
        this.fromState(NotificationStatusInt.DELIVERED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
        ;

        // Effective date state
        this.fromState(NotificationStatusInt.EFFECTIVE_DATE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET) //Multi destinatari
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
        ;

        // Viewed state
        this.fromState(NotificationStatusInt.VIEWED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.VIEWED, SINGLE_RECIPINET) //Multi Destinatari
                .withTimelineGoToState(TimelineElementCategoryInt.NOT_HANDLED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET) // TODO Aggiunto ma è da chiarire se è possibile questa casistica

                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.PAID, SINGLE_RECIPINET)
        ;

        this.fromState(NotificationStatusInt.UNREACHABLE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.UNREACHABLE, SINGLE_RECIPINET)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
        ;
        
        //FINAL STATE
        this.fromState(NotificationStatusInt.PAID)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOT_HANDLED, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
        ;
        this.fromState(NotificationStatusInt.CANCELLED);
        this.fromState(NotificationStatusInt.REFUSED);
    }

    NotificationStatusInt getStateTransition(NotificationStatusInt fromStatus, TimelineElementCategoryInt timelineRowType, boolean multiRecipient) throws PnInternalException {
        MapKey key = new MapKey(fromStatus, timelineRowType, multiRecipient);
        if (!isValidTransition(fromStatus, timelineRowType, multiRecipient, key)) {
            log.warn("Illegal input \"" + timelineRowType + "\" in state \"" + fromStatus + "\"");
            return fromStatus;
        }

        return getStatus(fromStatus, timelineRowType);
    }

    private boolean isValidTransition(NotificationStatusInt fromStatus, TimelineElementCategoryInt timelineRowType, boolean multiRecipient, MapKey key) {
        if (!this.mappings.containsKey(key)) {
            if(multiRecipient == FOR_MULTI_RECIPIENT) {
                MapKey mapKeySingleRecipient = new MapKey(fromStatus, timelineRowType, SINGLE_RECIPINET);
                return this.mappings.containsKey(mapKeySingleRecipient);
            }
            return false;
        }
        return true;
    }

    private NotificationStatusInt getStatus(NotificationStatusInt fromStatus, TimelineElementCategoryInt timelineRowType) {
        return this.mappings.get(new MapKey(fromStatus, timelineRowType, SINGLE_RECIPINET)).getStatus();
    }


    private InputMapper fromState(NotificationStatusInt fromStatus) {
        return new InputMapper(fromStatus);
    }


    private class InputMapper {

        private final NotificationStatusInt fromStatus;

        public InputMapper(NotificationStatusInt fromStatus) {
            this.fromStatus = fromStatus;
        }

        public InputMapper withTimelineGoToState(TimelineElementCategoryInt timelineRowType, NotificationStatusInt destinationStatus, boolean multiRecipient) {
            if( this.fromStatus.equals(NotificationStatusInt.DELIVERING) &&
                    TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW.equals(timelineRowType) &&
                    Boolean.TRUE.equals( pnDeliveryPushConfigs.getPaperMessageNotHandled()) ){
                //Solo per l'MVP in caso di FAILURE del workflow non avviene il passaggio di stato in DELIVERED ma resta in DELIVERING
                StateMap.this.mappings.put(new MapKey(fromStatus, timelineRowType, multiRecipient), new MapValue(NotificationStatusInt.DELIVERING));
            }else {
                StateMap.this.mappings.put(new MapKey(fromStatus, timelineRowType, multiRecipient), new MapValue(destinationStatus));
            }
            return this;
        }
    }

    @Value
    private static class MapKey {
        NotificationStatusInt status;
        TimelineElementCategoryInt timelineElementCategory;
        boolean multiRecipient;

    }

    @Value
    private static class MapValue {
        NotificationStatusInt status;
    }
}
