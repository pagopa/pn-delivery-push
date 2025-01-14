package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogWorfklowRecipientDeceasedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.dto.transition.TransitionRequest;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONSTATUSFAILED;

@Component
public class StatusUtils {
    private final StateMap stateMap;
    private final SmartMapper smartMapper;

    public StatusUtils(SmartMapper smartMapper){
        this.smartMapper = smartMapper;
        this.stateMap = new StateMap();
    }
    
    private static final NotificationStatusInt INITIAL_STATUS = NotificationStatusInt.IN_VALIDATION;
    // Attenzione: L'ordine in cui sono stati inseriti gli stati è importante per la logica di priorità
    private static final List<TimelineElementCategoryInt> COMPLETED_DELIVERY_WORKFLOW_CATEGORY = new ArrayList<>(Arrays.asList(
            //Completato con successo
            TimelineElementCategoryInt.DIGITAL_DELIVERY_CREATION_REQUEST, //Anche in caso di fallimento del digital workflow, la notifica si può considerare consegnata
            TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW,
            //Fallimento
            TimelineElementCategoryInt.COMPLETELY_UNREACHABLE,
            TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED
    ));

    
    public NotificationStatusInt getCurrentStatusFromNotification(NotificationInt notification, TimelineService timelineService) {
        Set<TimelineElementInternal> timelineElements =  timelineService.getTimeline(notification.getIun(), true);
        
        List<NotificationStatusHistoryElementInt> statusHistory = getStatusHistory( 
                timelineElements,
                notification.getRecipients().size(),
                notification.getSentAt()
        );

        return getCurrentStatus( statusHistory );
    }
    
    public NotificationStatusInt getCurrentStatus(List<NotificationStatusHistoryElementInt> statusHistory) {
        if (!statusHistory.isEmpty()) {
            return statusHistory.get(statusHistory.size() - 1).getStatus();
        } else {
            return INITIAL_STATUS;
        }
    }
    
    public List<NotificationStatusHistoryElementInt> getStatusHistory(Set<TimelineElementInternal> timelineElementList, 
                                                                      int numberOfRecipients, 
                                                                      Instant notificationCreatedAt) {

        //Map TimelineElementInternal per cambio timestamp con business timestamp
        Set<TimelineElementInternal> timelineElementListMapped = timelineElementList.stream()
                .map(elem -> smartMapper.mapTimelineInternal(elem, timelineElementList)).collect(Collectors.toSet());


        //La timeline ricevuta in ingresso è relativa a tutta la notifica e non al singolo recipient
        List<TimelineElementInternal> timelineByTimestampSorted = timelineElementListMapped.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    
        List<NotificationStatusHistoryElementInt> timelineHistory = new ArrayList<>();

        List<String> relatedTimelineElements = new ArrayList<>();
        List<TimelineElementCategoryInt> relatedCategoryElements = new ArrayList<>();

        Instant creationDateCurrentState = notificationCreatedAt;
        NotificationStatusInt currentState = INITIAL_STATUS;
        int numberOfCompletedWorkflow = 0;

        for (TimelineElementInternal timelineElement : timelineByTimestampSorted) {

            TimelineElementCategoryInt category = timelineElement.getCategory();


            if( COMPLETED_DELIVERY_WORKFLOW_CATEGORY.contains( category ) ) {
                //Vengono contati il numero di workflow completate per tutti i recipient, sia in caso di successo che di fallimento
                numberOfCompletedWorkflow += 1;
            }

            relatedCategoryElements.add( category );

            NotificationStatusInt nextState = computeStateAfterEvent(
                    timelineElement.getDetails(),
                    currentState,
                    category,
                    numberOfCompletedWorkflow,
                    numberOfRecipients,
                    relatedCategoryElements,
                    timelineByTimestampSorted
            );

            //Se lo stato corrente è diverso dal prossimo stato
            if (!Objects.equals(currentState, nextState)) {

                NotificationStatusHistoryElementInt statusHistoryElement = NotificationStatusHistoryElementInt.builder()
                        .status( currentState )
                        .activeFrom( creationDateCurrentState )
                        .relatedTimelineElements( relatedTimelineElements )
                        .build();

                //Viene aggiunto alla status history lo stato "precedente"
                timelineHistory.add(statusHistoryElement);
                //Viene azzerata la relatedTimelineElement
                relatedTimelineElements = new ArrayList<>();
                relatedCategoryElements = new ArrayList<>();
                //Ed aggiornata la creationDate
                creationDateCurrentState = timelineElement.getTimestamp();
            }

            //Viene aggiunto alla relatedTimelineElement l'elemento di timeline
            relatedTimelineElements.add( timelineElement.getElementId() );

            //Viene aggiornato il currentState nel caso in cui sia cambiato
            currentState = nextState;
        }
                 
        NotificationStatusHistoryElementInt statusHistoryElement = NotificationStatusHistoryElementInt.builder()
                .status( currentState )
                .activeFrom( creationDateCurrentState )
                .relatedTimelineElements( relatedTimelineElements )
                .build();
        timelineHistory.add(statusHistoryElement);

        return timelineHistory;
    }

    private NotificationStatusInt computeStateAfterEvent(
            TimelineElementDetailsInt details,
            NotificationStatusInt currentState,
            TimelineElementCategoryInt timelineElementCategory,
            int numberOfCompletedWorkflow,
            int numberOfRecipients,
            List<TimelineElementCategoryInt> relatedCategoryElements,
            List<TimelineElementInternal> timelineByTimestampSorted
    ) {
        NotificationStatusInt nextState;

        boolean multiRecipient = numberOfRecipients > 1;

        //(Gli stati ACCEPTED e DELIVERING sono gli stati in cui ci sono differenze di gestione per il multi destinatario, dunque prevedono una logica ad-hoc per il cambio stato)
        // Se sono nello stato ACCEPTED o DELIVERING e l'elemento di timeline preso in considerazione è uno degli stati di successo o fallimento del workflow ...
        if ( ( currentState.equals(NotificationStatusInt.ACCEPTED) || currentState.equals(NotificationStatusInt.DELIVERING) ) 
                &&
             ( COMPLETED_DELIVERY_WORKFLOW_CATEGORY.contains(timelineElementCategory) )
        ) {
            //... e il workflow è stato completato per tutti i recipient della notifica
            if( numberOfCompletedWorkflow == numberOfRecipients ){
                //... può essere ottenuto il nextState
                nextState =  getNextState(currentState, relatedCategoryElements, numberOfRecipients);
            }else {
                //... Altrimenti lo stato non cambia, bisogna attendere la fine del workflow per tutti i recipient
                nextState = currentState;
            }
        } else if(timelineElementCategory == TimelineElementCategoryInt.NOTIFICATION_VIEWED && multiRecipient) {
            return computeStateAfterViewEvent(details, currentState, timelineElementCategory, timelineByTimestampSorted);
        } else {
            //... Altrimenti lo stato viene calcolato normalmente dalla mappa
                nextState = stateMap.getStateTransition(
                        TransitionRequest.builder()
                                .fromStatus(currentState)
                                .timelineRowType(timelineElementCategory)
                                .multiRecipient(multiRecipient)
                                .build()
                );
        }
        
        return nextState;
    }

    private NotificationStatusInt getNextState(NotificationStatusInt currentState, List<TimelineElementCategoryInt> relatedCategoryElements, int numberOfRecipient) {
        boolean multiRecipient = numberOfRecipient > 1;

        TimelineElementCategoryInt category = pickCategoryByPriority(relatedCategoryElements);

        return stateMap.getStateTransition(TransitionRequest.builder()
                .fromStatus(currentState)
                .timelineRowType(category)
                .multiRecipient(multiRecipient)
                .build());
    }

    private static TimelineElementCategoryInt pickCategoryByPriority(List<TimelineElementCategoryInt> relatedCategories) {
        for (TimelineElementCategoryInt orderedCategory : COMPLETED_DELIVERY_WORKFLOW_CATEGORY) {
            if (relatedCategories.contains(orderedCategory)) {
                return orderedCategory;
            }
        }

        throw new PnInternalException("No end workflow category found", ERROR_CODE_DELIVERYPUSH_NOTIFICATIONSTATUSFAILED);
    }

    /**
     * Calcola il prossimo stato della notifica dopo un evento di visualizzazione.
     * In seguito all'introduzione degli elementi di timeline per il deceduto, la gestione deve prevedere che per una notifica
     * con multidestinatario, se un destinatario deceduto visualizza la notifica, lo stato non deve cambiare
     *
     * @param details The details of the timeline element.
     * @param currentState The current notification status.
     * @param timelineElementCategory The category of the timeline element (Should always be NOTIFICATION_VIEWED).
     * @param timelineByTimestampSorted The list of timeline elements sorted by timestamp.
     * @return The next notification status.
     * @throws PnInternalException If no timeline element is found with the given element ID.
     */
    private NotificationStatusInt computeStateAfterViewEvent(TimelineElementDetailsInt details, NotificationStatusInt currentState, TimelineElementCategoryInt timelineElementCategory, List<TimelineElementInternal> timelineByTimestampSorted) {
        int viewRecIdx = ((NotificationViewedDetailsInt) details).getRecIndex();

        if(isViewedByDeceasedRecipient(viewRecIdx, timelineByTimestampSorted)) {
            return currentState;
        }

        //... Altrimenti lo stato viene calcolato normalmente dalla mappa
        return stateMap.getStateTransition(
                TransitionRequest.builder()
                        .fromStatus(currentState)
                        .timelineRowType(timelineElementCategory)
                        .multiRecipient(true) // Questo metodo è raggiunto solo per multi recipient
                        .build()
        );
    }

    /**
     * Cerca su tutti gli elementi di timeline se per il recipient indicato è presente un evento di deceduto.
     * @param recIdx
     * @param timelineByTimestampSorted
     * @return
     */
    private boolean isViewedByDeceasedRecipient(int recIdx, List<TimelineElementInternal> timelineByTimestampSorted) {
        return timelineByTimestampSorted.stream()
                .filter(elementInternal -> elementInternal.getCategory().equals(TimelineElementCategoryInt.ANALOG_WORKFLOW_RECIPIENT_DECEASED))
                .map(elementInternal -> ((AnalogWorfklowRecipientDeceasedDetailsInt)elementInternal.getDetails()).getRecIndex())
                .anyMatch(deceasedRecIdx -> deceasedRecIdx == recIdx);
    }

}
