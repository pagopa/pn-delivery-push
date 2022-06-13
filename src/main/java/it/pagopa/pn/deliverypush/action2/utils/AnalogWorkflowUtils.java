package it.pagopa.pn.deliverypush.action2.utils;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class AnalogWorkflowUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    
    public AnalogWorkflowUtils(TimelineService timelineService,
                               TimelineUtils timelineUtils,
                               NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
    }

    /**
     * Get external channel last feedback information from timeline
     ** @return last sent feedback information
     */
    public SendPaperFeedbackDetails getLastTimelineSentFeedback(String iun, Integer recIndex) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(iun);

        Optional<SendPaperFeedbackDetails> sendPaperFeedbackDetailsOpt = timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, recIndex))
                .map(timelineElement -> SmartMapper.mapToClass(timelineElement.getDetails(), SendPaperFeedbackDetails.class))
                .findFirst();

        if (sendPaperFeedbackDetailsOpt.isPresent()) {
            return sendPaperFeedbackDetailsOpt.get();
        } else {
            log.error("Last send feedback is not available - iun {} id {}", iun, recIndex);
            throw new PnInternalException("Last send feedback is not available - iun " + iun + " id " + recIndex);
        }
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElementInternal el, Integer recIndex) {
        boolean availableAddressCategory = TimelineElementCategory.SEND_PAPER_FEEDBACK.equals(el.getCategory());
        if (availableAddressCategory) {
            SendPaperFeedbackDetails details = SmartMapper.mapToClass(el.getDetails(), SendPaperFeedbackDetails.class);
            return recIndex.equals(details.getRecIndex());
        }
        return false;
    }

    public void addAnalogFailureAttemptToTimeline(NotificationInt notification, int sentAttemptMade, List<LegalFactsId> attachmentKeys, PhysicalAddress newAddress, List<String> errors, SendPaperDetails sendPaperDetails) {
        addTimelineElement( 
                timelineUtils.buildAnalogFailureAttemptTimelineElement(notification, sentAttemptMade, attachmentKeys, newAddress, errors, sendPaperDetails),
                notification);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    public PhysicalAddress getPhysicalAddress(NotificationInt notification, Integer recIndex){
        NotificationRecipientInt notificationRecipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return notificationRecipient.getPhysicalAddress();
    }
}
