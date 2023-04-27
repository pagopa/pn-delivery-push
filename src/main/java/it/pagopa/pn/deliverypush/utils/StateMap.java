package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.transition.TransitionRequest;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
class StateMap {
    private static final boolean ONLY_MULTI_RECIPIENT = true; // il multi-destinatario comprende transizioni di stato AGGIUNTIVI al singolo destinatario
    private static final boolean SINGLE_RECIPINET = false;

    private final Map<MapKey, MapValue> mappings = new HashMap<>();

    public StateMap() {
        
        // Received state
        this.fromState(NotificationStatusInt.IN_VALIDATION)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST, NotificationStatusInt.IN_VALIDATION, SINGLE_RECIPINET)
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_ACCEPTED, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REQUEST_REFUSED, NotificationStatusInt.REFUSED, SINGLE_RECIPINET)
        ;
                
        this.fromState(NotificationStatusInt.ACCEPTED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_CREATION_REQUEST, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_CREATION_REQUEST, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.ACCEPTED, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_DIGITAL_DOMICILE, NotificationStatusInt.ACCEPTED, SINGLE_RECIPINET)

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
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_DIGITAL_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_CREATION_REQUEST, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Fallito workflow analogico, ci sarà l'elemento di timeline Completely unreachable che porta allo stato UNREACHABLE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Per le notifiche multi recipient potrebbe esserci il REFINEMENT anche in fase di DELIVERING (perchè la notifica potrebbe essere stata consegnata per un destinatario ma non per i restanti)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Con i MultiDestinatari potrebbe essere schedulato l'analog workflow anche in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Con i MultiDestinatari potrebbe essere inviato il messaggio di cortesia in delivering
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_CREATION_REQUEST, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Multi Destinatari
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Multi Destinatari
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Multi Destinatari
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Multi Destinatari
                .withTimelineGoToState(TimelineElementCategoryInt.SIMPLE_REGISTERED_LETTER_PROGRESS, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET) //Multi Destinatari
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.DELIVERING, SINGLE_RECIPINET)
                
                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.UNREACHABLE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
        ;

        // Delivered state
        this.fromState(NotificationStatusInt.DELIVERED)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SIMPLE_REGISTERED_LETTER_PROGRESS, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.DELIVERED, SINGLE_RECIPINET)

                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOT_HANDLED, NotificationStatusInt.CANCELLED, SINGLE_RECIPINET)
        ;

        // Effective date state
        this.fromState(NotificationStatusInt.EFFECTIVE_DATE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_DIGITAL_DOMICILE, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_CREATION_REQUEST, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT) 
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT) 
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT) 
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_CREATION_REQUEST, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT) 
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT) 
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT) 
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SIMPLE_REGISTERED_LETTER_PROGRESS, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)

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
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_DIGITAL_DOMICILE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SIMPLE_REGISTERED_LETTER_PROGRESS, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_CREATION_REQUEST, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_CREATION_REQUEST, NotificationStatusInt.VIEWED, SINGLE_RECIPINET) 
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOT_HANDLED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, ONLY_MULTI_RECIPIENT)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)

                //STATE CHANGE

        ;

        this.fromState(NotificationStatusInt.UNREACHABLE)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.UNREACHABLE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, NotificationStatusInt.UNREACHABLE, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PAYMENT, NotificationStatusInt.UNREACHABLE, SINGLE_RECIPINET)

                //STATE CHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.VIEWED, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.EFFECTIVE_DATE, SINGLE_RECIPINET)
        ;
        
        //FINAL STATE
        this.fromState(NotificationStatusInt.PAID)
                //STATE UNCHANGE
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_CREATION_REQUEST, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.AAR_GENERATION, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.GET_ADDRESS, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOTIFICATION_VIEWED, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.NOT_HANDLED, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_DIGITAL_DOMICILE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SIMPLE_REGISTERED_LETTER_PROGRESS, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_CREATION_REQUEST, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SCHEDULE_REFINEMENT, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.REFINEMENT, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
                .withTimelineGoToState(TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, NotificationStatusInt.PAID, SINGLE_RECIPINET)
        ;
        this.fromState(NotificationStatusInt.CANCELLED);
        this.fromState(NotificationStatusInt.REFUSED);
    }

    NotificationStatusInt getStateTransition(TransitionRequest transitionRequest) {
        NotificationStatusInt fromStatus = transitionRequest.getFromStatus();
        TimelineElementCategoryInt timelineRowType = transitionRequest.getTimelineRowType();
        
        return handleStateTransition(transitionRequest, fromStatus, timelineRowType);
    }

    private NotificationStatusInt handleStateTransition(TransitionRequest transitionRequest, NotificationStatusInt fromStatus, TimelineElementCategoryInt timelineRowType) {
        boolean multiRecipient = transitionRequest.isMultiRecipient();
        MapKey key = new MapKey(fromStatus, timelineRowType, multiRecipient);

        if(isValidTransition(key)) {
            return this.mappings.get(key).getStatus();
        }
        else {
            // se non è stata trovata la transizione nella mappa degli stati, controllo se siamo nel caso del multiRecipient,
            // perché potrebbe essere il caso in cui l'elemento è presente nella mappa con chiave multiRecipient = false
            // (StatiMultiDestinatario = StatiMonoDestinatario + statiAdHocMultiDestinatario)
            if(multiRecipient == ONLY_MULTI_RECIPIENT) {
                log.trace("Transition for only multiRecipient not found, trying for singleRecipient key");
                TransitionRequest transitionRequestForSingleRecipient = TransitionRequest.builder()
                        .fromStatus(transitionRequest.getFromStatus())
                        .timelineRowType(transitionRequest.getTimelineRowType())
                        .multiRecipient(SINGLE_RECIPINET)
                        .build();
                return getStateTransition(transitionRequestForSingleRecipient);
            }
            
            log.error("Illegal input \"" + timelineRowType + "\" in state \"" + fromStatus + "\"");
            return fromStatus;
        }
    }

    private boolean isValidTransition(MapKey mapKey) {
        return this.mappings.containsKey(mapKey);
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
            StateMap.this.mappings.put(new MapKey(fromStatus, timelineRowType, multiRecipient), new MapValue(destinationStatus));
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
