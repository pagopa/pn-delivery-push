package it.pagopa.pn.deliverypush.action.analogworkflow;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.BaseAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_FEEDBACKNOTFOUND;

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
     * * @return last sent feedback information
     */
    public SendAnalogFeedbackDetailsInt getLastTimelineSentFeedback(String iun, Integer recIndex) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(iun, true);

        Optional<SendAnalogFeedbackDetailsInt> sendPaperFeedbackDetailsOpt = timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, recIndex))
                .map(timelineElement -> (SendAnalogFeedbackDetailsInt) timelineElement.getDetails())
                .findFirst();

        if (sendPaperFeedbackDetailsOpt.isPresent()) {
            return sendPaperFeedbackDetailsOpt.get();
        } else {
            log.error("Last send feedback is not available - iun {} id {}", iun, recIndex);
            throw new PnInternalException("Last send feedback is not available - iun " + iun + " id " + recIndex, ERROR_CODE_DELIVERYPUSH_FEEDBACKNOTFOUND);
        }
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElementInternal el, Integer recIndex) {
        boolean availableAddressCategory = TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK.equals(el.getCategory());
        if (availableAddressCategory) {
            SendAnalogFeedbackDetailsInt details = (SendAnalogFeedbackDetailsInt) el.getDetails();
            return recIndex.equals(details.getRecIndex());
        }
        return false;
    }

    public String addAnalogFailureAttemptToTimeline(NotificationInt notification, int sentAttemptMade, List<AttachmentDetailsInt> attachments,
                                                  BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildAnalogFailureAttemptTimelineElement(notification, sentAttemptMade, attachments, sendPaperDetails, sendEventInt);

        addTimelineElement(timelineElementInternal,
                notification);

        return timelineElementInternal.getElementId();
    }


    public void addAnalogProgressAttemptToTimeline(NotificationInt notification, int recIndex, int sentAttemptMade, List<AttachmentDetailsInt> attachments,
                                                   BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt) {
        int progressIndex = getPreviousTimelineProgress(notification, recIndex, sentAttemptMade).size() + 1;

        addTimelineElement(
                timelineUtils.buildAnalogProgressTimelineElement(notification, sentAttemptMade, attachments, progressIndex, sendPaperDetails, sendEventInt),
                notification);
    }

    public String addAnalogSuccessAttemptToTimeline(NotificationInt notification, int sentAttemptMade, List<AttachmentDetailsInt> attachments,
                                                    BaseAnalogDetailsInt sendPaperDetails, SendEventInt sendEventInt) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildAnalogSuccessAttemptTimelineElement(notification, sentAttemptMade, attachments, sendPaperDetails, sendEventInt);

        addTimelineElement(timelineElementInternal,
                notification);

        return timelineElementInternal.getElementId();
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
    
    public PhysicalAddressInt getPhysicalAddress(NotificationInt notification, Integer recIndex){
        NotificationRecipientInt notificationRecipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return notificationRecipient.getPhysicalAddress();
    }


    private Set<TimelineElementInternal> getPreviousTimelineProgress(NotificationInt notification,
                                                                    int recIndex, int attemptMade){
        // per calcolare il prossimo progressIndex, devo necessariamente recuperare dal DB tutte le timeline relative a iun/recindex/source/tentativo
        String elementIdForSearch = TimelineEventId.SEND_ANALOG_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(attemptMade)
                        .progressIndex(-1)  // passando -1 non verrà inserito nell'id timeline, permettendo la ricerca iniziaper
                        .build()
        );
        return this.timelineService.getTimelineByIunTimelineId(notification.getIun(), elementIdForSearch, false);
    }
}
