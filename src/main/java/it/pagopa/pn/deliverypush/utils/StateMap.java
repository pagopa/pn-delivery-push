package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
class StateMap {
    private final Map<MapKey, MapValue> mappings = new HashMap<>();

    public StateMap() {
        // Received state
        this.fromState(NotificationStatusInt.IN_VALIDATION)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_ACCEPTED, NotificationStatusInt.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_REFUSED, NotificationStatusInt.REFUSED)
        ;
                
        this.fromState(NotificationStatusInt.ACCEPTED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.ACCEPTED)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.UNREACHABLE) //Casista tutti gli indirizzi digitali e analogici non sono presenti
        ;

        // Delivering state
        this.fromState(NotificationStatusInt.DELIVERING)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.DELIVERING) //Fallito workflow analogico, ci sarà l'elemento di timeline Completely unreachable che porta allo stato UNREACHABLE 
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.DELIVERING) //Per le notifiche multi recipient potrebbe esserci il REFINEMENT anche in fase di DELIVERING (perchè la notifica potrebbe essere stata consegnata per un destinatario ma non per i restanti)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.DELIVERING) //Con i MultiDestinatari potrebbe essere schedulato l'analog workflow anche in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.DELIVERING) //Con i MultiDestinatari potrebbe essere inviato il messaggio di cortesia in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.DELIVERING) //Multi Destinatari

                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.UNREACHABLE)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatusInt.DELIVERED)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.DELIVERED)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, NotificationStatusInt.DELIVERED)
        ;

        // Delivered state
        this.fromState(NotificationStatusInt.DELIVERED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.DELIVERED)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE)
        ;

        // Effective date state
        this.fromState(NotificationStatusInt.EFFECTIVE_DATE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE) //Multi destinatari
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.PAID)
        ;

        // Viewed state
        this.fromState(NotificationStatusInt.VIEWED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.VIEWED )
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.VIEWED )
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.VIEWED) //Multi Destinatari

        
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.PAID)
        ;

        this.fromState(NotificationStatusInt.UNREACHABLE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.UNREACHABLE)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE)
        ;

        this.fromState(NotificationStatusInt.PAID);
        this.fromState(NotificationStatusInt.REFUSED);
    }

    NotificationStatusInt getStateTransition(NotificationStatusInt fromStatus, TimelineElementCategoryInt timelineRowType) throws PnInternalException {
        MapKey key = new MapKey(fromStatus, timelineRowType);
        if (!this.mappings.containsKey(key)) {
            log.warn("Illegal input \"" + timelineRowType + "\" in state \"" + fromStatus + "\"");
            return fromStatus;
        }

        final MapValue mapValue = this.mappings.get(key);
        return mapValue.getStatus();
    }


    private InputMapper fromState(NotificationStatusInt fromStatus) {
        return new InputMapper(fromStatus);
    }


    private class InputMapper {

        private final NotificationStatusInt fromStatus;

        public InputMapper(NotificationStatusInt fromStatus) {
            this.fromStatus = fromStatus;
        }

        public InputMapper withTimelineGoToState(TimelineElementCategoryInt timelineRowType, NotificationStatusInt destinationStatus) {
            StateMap.this.mappings.put(new MapKey(fromStatus, timelineRowType), new MapValue(destinationStatus));
            return this;
        }
    }

    @Value
    private static class MapKey {
        private final NotificationStatusInt status;
        private final TimelineElementCategoryInt timelineElementCategory;
    }

    @Value
    private static class MapValue {
        private final NotificationStatusInt status;
    }
}
