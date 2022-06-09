package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
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
                .withTimelineGoToState(TimelineElementCategory.REQUEST_ACCEPTED, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategory.REQUEST_REFUSED, NotificationStatus.REFUSED)
        ;
                
        this.fromState(NotificationStatus.ACCEPTED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategory.AAR_GENERATION, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategory.SEND_COURTESY_MESSAGE, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategory.GET_ADDRESS, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategory.PUBLIC_REGISTRY_CALL, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, NotificationStatus.ACCEPTED)
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, NotificationStatus.ACCEPTED)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategory.SEND_DIGITAL_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SEND_ANALOG_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.COMPLETELY_UNREACHABLE, NotificationStatus.UNREACHABLE) //Casista tutti gli indirizzi digitali e analogici non sono presenti
        ;

        // Delivering state
        this.fromState(NotificationStatus.DELIVERING)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategory.SEND_PAPER_FEEDBACK, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SEND_DIGITAL_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SEND_DIGITAL_DOMICILE_FEEDBACK, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SEND_ANALOG_DOMICILE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.GET_ADDRESS, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.PUBLIC_REGISTRY_CALL, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SEND_DIGITAL_FEEDBACK, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, NotificationStatus.DELIVERING) //Fallito workflow analogico, ci sarà l'elemento di timeline Completely unreachable che porta allo stato UNREACHABLE 
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_REFINEMENT, NotificationStatus.DELIVERING) //Per le notifiche multi recipient potrebbe esserci il REFINEMENT anche in fase di DELIVERING (perchè la notifica potrebbe essere stata consegnata per un destinatario ma non per i restanti)
                .withTimelineGoToState(TimelineElementCategory.REFINEMENT, NotificationStatus.DELIVERING)
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, NotificationStatus.DELIVERING) //Con i MultiDestinatari potrebbe essere schedulato l'analog workflow anche in delivering
                .withTimelineGoToState(TimelineElementCategory.SEND_COURTESY_MESSAGE, NotificationStatus.DELIVERING) //Con i MultiDestinatari potrebbe essere inviato il messaggio di cortesia in delivering


                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategory.COMPLETELY_UNREACHABLE, NotificationStatus.UNREACHABLE)
                .withTimelineGoToState(TimelineElementCategory.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, NotificationStatus.DELIVERED)
                .withTimelineGoToState(TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, NotificationStatus.DELIVERED)
                .withTimelineGoToState(TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW, NotificationStatus.DELIVERED)
        ;

        // Delivered state
        this.fromState(NotificationStatus.DELIVERED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_REFINEMENT, NotificationStatus.DELIVERED)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategory.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.REFINEMENT, NotificationStatus.EFFECTIVE_DATE)
        ;

        // Effective date state
        this.fromState(NotificationStatus.EFFECTIVE_DATE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategory.REFINEMENT, NotificationStatus.EFFECTIVE_DATE) //Multi destinatari
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategory.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.PAYMENT, NotificationStatus.PAID)
        ;

        // Viewed state
        this.fromState(NotificationStatus.VIEWED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategory.SEND_COURTESY_MESSAGE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.GET_ADDRESS, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.PUBLIC_REGISTRY_CALL, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SEND_PAPER_FEEDBACK, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SEND_DIGITAL_DOMICILE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SEND_DIGITAL_DOMICILE_FEEDBACK, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SEND_ANALOG_DOMICILE, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SEND_DIGITAL_FEEDBACK, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_REFINEMENT, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.REFINEMENT, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, NotificationStatus.VIEWED )
                .withTimelineGoToState(TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, NotificationStatus.VIEWED )

        
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategory.PAYMENT, NotificationStatus.PAID)
        ;

        this.fromState(NotificationStatus.UNREACHABLE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategory.SCHEDULE_REFINEMENT, NotificationStatus.UNREACHABLE)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategory.NOTIFICATION_VIEWED, NotificationStatus.VIEWED)
                .withTimelineGoToState(TimelineElementCategory.REFINEMENT, NotificationStatus.EFFECTIVE_DATE)
        ;

        this.fromState(NotificationStatus.PAID);
        this.fromState(NotificationStatus.REFUSED);
    }

    NotificationStatus getStateTransition(NotificationStatus fromStatus, TimelineElementCategory timelineRowType) throws PnInternalException {
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

        public InputMapper withTimelineGoToState(TimelineElementCategory timelineRowType, NotificationStatus destinationStatus) {
            StateMap.this.mappings.put(new MapKey(fromStatus, timelineRowType), new MapValue(destinationStatus));
            return this;
        }
    }

    @Value
    private static class MapKey {
        private final NotificationStatus status;
        private final TimelineElementCategory timelineElementCategory;
    }

    @Value
    private static class MapValue {
        private final NotificationStatus status;
    }
}
