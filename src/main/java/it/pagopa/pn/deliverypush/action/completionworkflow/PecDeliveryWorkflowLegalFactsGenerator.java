package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PecDeliveryWorkflowLegalFactsGenerator {
    private final TimelineService timelineService;
    private final SaveLegalFactsService saveLegalFactsService;
    private final NotificationUtils notificationUtils;

    public PecDeliveryWorkflowLegalFactsGenerator(TimelineService timelineService, 
                                                  SaveLegalFactsService saveLegalFactsService, 
                                                  NotificationUtils notificationUtils
    ) {
        this.timelineService = timelineService;
        this.saveLegalFactsService = saveLegalFactsService;
        this.notificationUtils = notificationUtils;
    }

    public String generatePecDeliveryWorkflowLegalFact(NotificationInt notification, Integer recIndex, EndWorkflowStatus status, Instant completionWorkflowDate) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(notification.getIun(), true);

        List<TimelineElementInternal> timelineByTimestampSorted = timeline.stream()
                .sorted(Comparator.comparing(TimelineElementInternal::getTimestamp))
                .collect(Collectors.toList());

        List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel = new ArrayList<>();

        for(TimelineElementInternal element : timelineByTimestampSorted){
            if(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK.equals(element.getCategory())){
                getSpecificDetailRecipient(element, recIndex).ifPresent(
                        details -> listFeedbackFromExtChannel.add((SendDigitalFeedbackDetailsInt) details)
                );
            }
        }

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return saveLegalFactsService.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient, status, completionWorkflowDate);
    }

    private Optional<RecipientRelatedTimelineElementDetails> getSpecificDetailRecipient(TimelineElementInternal element, int recIndex){
        if (element.getDetails() instanceof RecipientRelatedTimelineElementDetails) {
            RecipientRelatedTimelineElementDetails details = (RecipientRelatedTimelineElementDetails) element.getDetails();
            if( recIndex == details.getRecIndex()){
                return Optional.of(details);
            }
        }
        return Optional.empty();
    }
}
