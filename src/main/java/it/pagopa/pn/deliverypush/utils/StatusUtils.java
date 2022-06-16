package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StatusUtils {

    private static final NotificationStatus INITIAL_STATUS = NotificationStatus.IN_VALIDATION;
    private static final Set<TimelineElementCategoryInt> SUCCES_DELIVERY_WORKFLOW_CATEGORY = new HashSet<>(Arrays.asList(
            //Completato con successo
            TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW,
            TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, //Anche in caso di fallimento del digital workflow, la notifica si può considerare consegnata 
            TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW
    ));
    
    private static final Set<TimelineElementCategoryInt> FAILURE_DELIVERY_WORKFLOW_CATEGORY = new HashSet<>(List.of(
            TimelineElementCategoryInt.COMPLETELY_UNREACHABLE
    ));

    private final StateMap stateMap = new StateMap();

    public NotificationStatus getCurrentStatus(List<NotificationStatusHistoryElement> statusHistory) {
        if (!statusHistory.isEmpty()) {
            return statusHistory.get(statusHistory.size() - 1).getStatus();
        } else {
            return INITIAL_STATUS;
        }
    }
    
    public List<NotificationStatusHistoryElement> getStatusHistory( //
                                                                    Set<TimelineElementInternal> timelineElementList, //
                                                                    int numberOfRecipients, //
                                                                    Instant notificationCreatedAt //
    ) {
        //La timeline ricevuta in ingresso è relativa a tutta la notifica e non al singolo recipient
        List<TimelineElementInternal> timelineByTimestampSorted = timelineElementList.stream()
                .sorted(Comparator.comparing(TimelineElementInternal::getTimestamp))
                .collect(Collectors.toList());
    
        List<NotificationStatusHistoryElement> timelineHistory = new ArrayList<>();

        List<String> relatedTimelineElements = new ArrayList<>();
        List<TimelineElementCategoryInt> relatedCategoryElements = new ArrayList<>();

        Instant creationDateCurrentState = notificationCreatedAt;
        NotificationStatus currentState = INITIAL_STATUS;
        int numberOfCompletedWorkflow = 0;

        for (TimelineElementInternal timelineElement : timelineByTimestampSorted) {
            TimelineElementCategoryInt category = timelineElement.getCategory();
            
            if( SUCCES_DELIVERY_WORKFLOW_CATEGORY.contains( category ) || FAILURE_DELIVERY_WORKFLOW_CATEGORY.contains( category ) ) {
                //Vengono contati il numero di workflow completate per entrambi i recipient, sia in caso di successo che di fallimento
                numberOfCompletedWorkflow += 1;
            }
            
            relatedCategoryElements.add( timelineElement.getCategory() );

            NotificationStatus nextState = computeStateAfterEvent(
                        currentState, category, numberOfCompletedWorkflow, numberOfRecipients, relatedCategoryElements);

            //Se lo stato corrente è diverso dal prossimo stato
            if (!Objects.equals(currentState, nextState)) {
                
                NotificationStatusHistoryElement statusHistoryElement = NotificationStatusHistoryElement.builder()
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
                 
        NotificationStatusHistoryElement statusHistoryElement = NotificationStatusHistoryElement.builder()
                .status( currentState )
                .activeFrom( creationDateCurrentState )
                .relatedTimelineElements( relatedTimelineElements )
                .build();
        timelineHistory.add(statusHistoryElement);

        return timelineHistory;
    }

    private NotificationStatus computeStateAfterEvent( 
                                                       NotificationStatus currentState, 
                                                       TimelineElementCategoryInt timelineElementCategory,
                                                       int numberOfCompletedWorkflow,
                                                       int numberOfRecipients,
                                                       List<TimelineElementCategoryInt> relatedCategoryElements
    ) {
        NotificationStatus nextState;

        //(Gli stati ACCEPTED e DELIVERING sono gli stati in cui ci sono differenze di gestione per il multi destinatario, dunque prevedono una logica ad-hoc per il cambio stato)
        // Se sono nello stato ACCEPTED o DELIVERING e l'elemento di timeline preso in considerazione è uno degli stati di successo o fallimento del workflow ...
        if ( ( currentState.equals(NotificationStatus.ACCEPTED) || currentState.equals(NotificationStatus.DELIVERING) ) 
                &&
             ( SUCCES_DELIVERY_WORKFLOW_CATEGORY.contains(timelineElementCategory) || FAILURE_DELIVERY_WORKFLOW_CATEGORY.contains(timelineElementCategory) )
        ) {
            //... e il workflow è stato completato per tutti i recipient della notifica
            if( numberOfCompletedWorkflow == numberOfRecipients ){
                //... può essere ottenuto il nextState
                nextState =  getNextState(currentState, relatedCategoryElements, numberOfRecipients);
            }else {
                //... Altrimenti lo stato non cambia, bisogna attendere la fine del workflow per tutti i recipient
                nextState = currentState;
            }
        } else {
            //... Altrimenti lo stato viene calcolato normalmente dalla mappa
                nextState = stateMap.getStateTransition(currentState, timelineElementCategory);
        }
        
        return nextState;
    }

    private NotificationStatus getNextState(NotificationStatus currentState, List<TimelineElementCategoryInt> relatedCategoryElements, int numberOfRecipient) {
        int failureWorkflow = 0;
        
        //Viene effettuato un ciclo su tutti gli elementi relati allo stato corrente
        for (TimelineElementCategoryInt category : relatedCategoryElements){
            
            //Se almeno per un recipient il workflow è andato a buon fine
            if( SUCCES_DELIVERY_WORKFLOW_CATEGORY.contains(category) ) {
                //Viene ottenuto lo stato relato alla category di successo
                return stateMap.getStateTransition(currentState, category);
                
                //Se per tutti i recipient il workflow è fallito
            }else if( FAILURE_DELIVERY_WORKFLOW_CATEGORY.contains(category) ) {
                failureWorkflow +=1;
                if( failureWorkflow == numberOfRecipient) {
                    
                    //Viene ottenuto lo stato relato alla category di fallimento
                    return stateMap.getStateTransition(currentState, category);
                }
            }
        }
        
        throw new PnInternalException("situazione anomala");
    }

}
