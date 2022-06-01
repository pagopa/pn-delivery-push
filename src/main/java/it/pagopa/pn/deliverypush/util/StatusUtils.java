package it.pagopa.pn.deliverypush.util;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StatusUtils {

    private static final NotificationStatus INITIAL_STATUS = NotificationStatus.IN_VALIDATION;
    private static final Set<TimelineElementCategory> SUCCES_DELIVERY_WORKFLOW_CATEGORY = new HashSet<>(Arrays.asList(
            //Completato con successo
            TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, 
            TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, //Anche in caso di fallimento del digital workflow, la notifica si può considerare consegnata 
            TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW
    ));
    
    private static final Set<TimelineElementCategory> FAILURE_DELIVERY_WORKFLOW_CATEGORY = new HashSet<>(List.of(
            TimelineElementCategory.COMPLETELY_UNREACHABLE
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
                .sorted(Comparator.comparing(TimelineElement::getTimestamp))
                .collect(Collectors.toList());
    
        List<NotificationStatusHistoryElement> timelineHistory = new ArrayList<>();

        List<String> relatedTimelineElements = new ArrayList<>();
        List<TimelineElementCategory> relatedCategoryElements = new ArrayList<>();

        Instant creationDateCurrentState = notificationCreatedAt;
        NotificationStatus currentState = INITIAL_STATUS;
        int numberOfCompletedWorkflow = 0;

        for (TimelineElementInternal timelineElement : timelineByTimestampSorted) {
            TimelineElementCategory category = timelineElement.getCategory();
            
            if( SUCCES_DELIVERY_WORKFLOW_CATEGORY.contains( category ) || FAILURE_DELIVERY_WORKFLOW_CATEGORY.contains( category ) ) {
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
                                                       TimelineElementCategory timelineElementCategory,
                                                       int numberOfCompletedWorkflow,
                                                       int numberOfRecipients,
                                                       List<TimelineElementCategory> relatedCategoryElements
    ) {
        NotificationStatus nextState;
        
        if ( ( currentState.equals(NotificationStatus.ACCEPTED) || currentState.equals(NotificationStatus.DELIVERING) ) 
                &&
             ( SUCCES_DELIVERY_WORKFLOW_CATEGORY.contains(timelineElementCategory) || FAILURE_DELIVERY_WORKFLOW_CATEGORY.contains(timelineElementCategory) )
        ) {
            if( numberOfCompletedWorkflow == numberOfRecipients ){
                nextState =  getNextState(currentState, relatedCategoryElements, numberOfRecipients);

            }else {
                nextState = currentState;
            }
        } else {
                nextState = stateMap.getStateTransition(currentState, timelineElementCategory);
        }
        
        return nextState;
    }

    private NotificationStatus getNextState(NotificationStatus currentState, List<TimelineElementCategory> relatedCategoryElements, int numberOfRecipient) {
        int failureWorkflow = 0;
        
        for (TimelineElementCategory category : relatedCategoryElements){
            if( SUCCES_DELIVERY_WORKFLOW_CATEGORY.contains(category) ) {
                return stateMap.getStateTransition(currentState, category);
                
            }else if( FAILURE_DELIVERY_WORKFLOW_CATEGORY.contains(category) ) {
                failureWorkflow +=1;
                if( failureWorkflow == numberOfRecipient) {
                    return stateMap.getStateTransition(currentState, category);
                }
            }
        }
        
        throw new PnInternalException("situazione anomala");
    }

}
