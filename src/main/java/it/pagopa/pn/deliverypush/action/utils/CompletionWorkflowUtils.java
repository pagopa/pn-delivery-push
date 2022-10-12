package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CompletionWorkflowUtils {
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final TimelineService timelineService;
    private final SaveLegalFactsService saveLegalFactsService;
    private final NotificationUtils notificationUtils;
    
    public CompletionWorkflowUtils(PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                   TimelineService timelineService,
                                   SaveLegalFactsService saveLegalFactsService,
                                   NotificationUtils notificationUtils) {
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
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
        PhysicalAddressInt sendRegisteredLetterAddress = null;

        for(TimelineElementInternal element : timelineByTimestampSorted){
            if(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK.equals(element.getCategory())){
                getSpecificDetailRecipient(element, recIndex).ifPresent(
                        details -> listFeedbackFromExtChannel.add((SendDigitalFeedbackDetailsInt) details)
                );
            } else {
                if(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER.equals(element.getCategory())){
                    Optional<RecipientRelatedTimelineElementDetails> opt = getSpecificDetailRecipient(element, recIndex);
                    if(opt.isPresent()){
                        SimpleRegisteredLetterDetailsInt simpleRegisteredLetterDetails = (SimpleRegisteredLetterDetailsInt) opt.get();
                        sendRegisteredLetterAddress = simpleRegisteredLetterDetails.getPhysicalAddress();
                    }
                }
            }
        }

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return saveLegalFactsService.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient, status, completionWorkflowDate, sendRegisteredLetterAddress);
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
    
    public Instant getSchedulingDate(Instant completionWorkflowDate, Duration scheduleTime, String iun) {
        String notificationNonVisibilityTime = pnDeliveryPushConfigs.getTimeParams().getNotificationNonVisibilityTime();
        String[] arrayTime = notificationNonVisibilityTime.split(":");
        int hour = Integer.parseInt(arrayTime[0]);
        int minute = Integer.parseInt(arrayTime[1]);
        int second = 0;
        int nanoOfSecond = 0;

        log.debug("Start getSchedulingDate with completionWorkflowDate={} scheduleTime={} notificationNonVisibilityTime={}:{}:{}:{} - iun={}",
                completionWorkflowDate, scheduleTime, hour, minute, second, nanoOfSecond, iun);

        ZonedDateTime notificationDateTime = DateFormatUtils.parseInstantToZonedDateTime(completionWorkflowDate);
        ZonedDateTime notificationNonVisibilityDateTime = DateFormatUtils.setSpecificTimeToDate(notificationDateTime, hour, minute, second, nanoOfSecond);

        log.debug("Formatted notificationDateTime={} and notificationNonVisibilityDateTime={} - iun={}", notificationDateTime, notificationNonVisibilityDateTime, iun);

        if (notificationDateTime.isAfter(notificationNonVisibilityDateTime)){
            Duration timeToAddToScheduledTime = pnDeliveryPushConfigs.getTimeParams().getTimeToAddInNonVisibilityTimeCase();
            scheduleTime = scheduleTime.plus(timeToAddToScheduledTime);
            log.debug("NotificationDateTime is after notificationNonVisibilityDateTime, need to add {} day to schedulingTime. scheduleTime={} - iun={}", timeToAddToScheduledTime, scheduleTime, iun);
        } else {
            log.debug("NotificationDateTime is not after notificationNonVisibilityDateTime, don't need to add any day to schedulingTime. scheduleTime={} - iun={}", scheduleTime, iun);
        }

        Instant schedulingDate = completionWorkflowDate.plus(scheduleTime);

        log.info("Scheduling Date is {} - iun={}", schedulingDate, iun);

        return schedulingDate;
    }

    public void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
