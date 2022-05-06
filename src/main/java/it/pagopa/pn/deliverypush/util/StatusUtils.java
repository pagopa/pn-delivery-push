package it.pagopa.pn.deliverypush.util;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StatusUtils {

    private static final NotificationStatus INITIAL_STATUS = NotificationStatus.IN_VALIDATION;
    private static final Set<TimelineElementCategory> END_OF_DELIVERY_WORKFLOW = new HashSet<>(Arrays.asList(
      TimelineElementCategory.END_OF_DIGITAL_DELIVERY_WORKFLOW,
      TimelineElementCategory.END_OF_ANALOG_DELIVERY_WORKFLOW
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
                                                                    Set<TimelineElement> timelineElementList, //
                                                                    int numberOfRecipients, //
                                                                    Instant notificationCreatedAt //
    ) {
        List<TimelineElement> timelineByTimestampSorted = timelineElementList.stream()
                .sorted(Comparator.comparing(TimelineElement::getTimestamp))
                .collect(Collectors.toList());

        List<NotificationStatusHistoryElement> timelineHistory = new ArrayList<>();

        List<String> relatedTimelineElements = new ArrayList<>();
        Instant currentStateStart = notificationCreatedAt;
        NotificationStatus currentState = INITIAL_STATUS;
        int numberOfEndedDeliveryWorkflows = 0;


        for (TimelineElement timelineElement : timelineByTimestampSorted) {
            TimelineElementCategory category = timelineElement.getCategory();
            if( END_OF_DELIVERY_WORKFLOW.contains( category ) ) {
                numberOfEndedDeliveryWorkflows += 1;
            }

            NotificationStatus nextState = computeStateAfterEvent(
                        currentState, category, numberOfEndedDeliveryWorkflows, numberOfRecipients);

            if (!Objects.equals(currentState, nextState)) {
                NotificationStatusHistoryElement statusHistoryElement = NotificationStatusHistoryElement.builder()
                        .status( currentState )
                        .activeFrom( currentStateStart )
                        .relatedTimelineElements( relatedTimelineElements )
                        .build();
                timelineHistory.add(statusHistoryElement);

                relatedTimelineElements = new ArrayList<>();
                currentStateStart = timelineElement.getTimestamp();
            }

            relatedTimelineElements.add( timelineElement.getElementId() );

            currentState = nextState;
        }

        NotificationStatusHistoryElement statusHistoryElement = NotificationStatusHistoryElement.builder()
                .status( currentState )
                .activeFrom( currentStateStart )
                .relatedTimelineElements( relatedTimelineElements )
                .build();
        timelineHistory.add(statusHistoryElement);

        return timelineHistory;
    }

    private NotificationStatus computeStateAfterEvent(  //
                                                       NotificationStatus currentState, //
                                                       TimelineElementCategory timelineElementCategory, //
                                                       int numberOfEndedDigitalWorkflows, //
                                                       int numberOfRecipients //
    ) {
        NotificationStatus nextState;
        if (currentState.equals(NotificationStatus.DELIVERING)) {
            if( timelineElementCategory.equals(TimelineElementCategory.END_OF_DIGITAL_DELIVERY_WORKFLOW) ) {
                if( numberOfEndedDigitalWorkflows == numberOfRecipients ) {
                    nextState = stateMap.getStateTransition(currentState, timelineElementCategory);
                }
                else {
                    nextState = currentState;
                }
            }
            else {
                nextState = stateMap.getStateTransition(currentState, timelineElementCategory);
            }
        } else {
            nextState = stateMap.getStateTransition(currentState, timelineElementCategory);
        }
        return nextState;
    }

}
