package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
class StateMap {
    private final Map<MapKey, MapValue> mappings = new HashMap<>();

    public StateMap() {
        // Received state
        this.fromState(NotificationStatus.IN_VALIDATION)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_ACCEPTED, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_REFUSED, NotificationStatus.REFUSED)
        ;
                
        this.fromState(NotificationStatus.ACCEPTED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatus.ACCEPTED)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatus.UNREACHABLE) //Casista tutti gli indirizzi digitali e analogici non sono presenti
        ;

        // Delivering state
        this.fromState(NotificationStatus.DELIVERING)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatus.DELIVERING) //Fallito workflow analogico, ci sarà l'elemento di timeline Completely unreachable che porta allo stato UNREACHABLE 
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatus.DELIVERING) //Per le notifiche multi recipient potrebbe esserci il REFINEMENT anche in fase di DELIVERING (perchè la notifica potrebbe essere stata consegnata per un destinatario ma non per i restanti)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatus.DELIVERING) //Con i MultiDestinatari potrebbe essere schedulato l'analog workflow anche in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatus.DELIVERING) //Con i MultiDestinatari potrebbe essere inviato il messaggio di cortesia in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatus.DELIVERING) //Multi Destinatari

                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatus.UNREACHABLE)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatus.DELIVERED)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatus.DELIVERED)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, NotificationStatus.DELIVERED)
        ;

        // Delivered state
        this.fromState(NotificationStatus.DELIVERED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatus.DELIVERED)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatus.EFFECTIVE_DATE)
        ;

        // Effective date state
        this.fromState(NotificationStatus.EFFECTIVE_DATE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatus.EFFECTIVE_DATE) //Multi destinatari
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatus.PAID)
        ;

        // Viewed state
        this.fromState(NotificationStatus.VIEWED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatus.VIEWED )
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatus.VIEWED )
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatus.VIEWED) //Multi Destinatari

        
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatus.PAID)
        ;

        this.fromState(NotificationStatus.UNREACHABLE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatus.UNREACHABLE)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatus.EFFECTIVE_DATE)
        ;

        this.fromState(NotificationStatus.PAID);
        this.fromState(NotificationStatus.REFUSED);
    }

    NotificationStatus getStateTransition(NotificationStatus fromStatus, TimelineElementCategoryInt timelineRowType) throws PnInternalException {
        MapKey key = new MapKey(fromStatus, timelineRowType);
        if (!this.mappings.containsKey(key)) {
            log.warn("Illegal input \"" + timelineRowType + "\" in state \"" + fromStatus + "\"");
            return fromStatus;
        }

        final MapValue mapValue = this.mappings.get(key);
        return mapValue.getStatus();
    }


    private InputMapper fromState(NotificationStatus fromStatus) {
        return new InputMapper(fromStatus);
    }


    private class InputMapper {

        private final NotificationStatus fromStatus;

        public InputMapper(NotificationStatus fromStatus) {
            this.fromStatus = fromStatus;
        }

        public InputMapper withTimelineGoToState(TimelineElementCategoryInt timelineRowType, NotificationStatus destinationStatus) {
            StateMap.this.mappings.put(new MapKey(fromStatus, timelineRowType), new MapValue(destinationStatus));
            return this;
        }
    }

    @Value
    private static class MapKey {
        private final NotificationStatus status;
        private final TimelineElementCategoryInt timelineElementCategory;
    }

    @Value
    private static class MapValue {
        private final NotificationStatus status;
    }
}
