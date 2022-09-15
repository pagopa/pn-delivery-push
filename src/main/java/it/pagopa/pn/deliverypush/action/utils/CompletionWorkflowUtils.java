package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
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

    public String generatePecDeliveryWorkflowLegalFact(NotificationInt notification, Integer recIndex) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(notification.getIun());

        List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel = timeline.stream()
                .filter(timelineElement -> filterSendDigitalFeedbackAndTaxId(timelineElement, recIndex))
                .map(timelineElement -> (SendDigitalFeedbackDetailsInt) timelineElement.getDetails())
                .collect(Collectors.toList());

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return saveLegalFactsService.savePecDeliveryWorkflowLegalFact(listFeedbackFromExtChannel, notification, recipient);
    }

    private boolean filterSendDigitalFeedbackAndTaxId(TimelineElementInternal el, Integer recIndex) {
        boolean availableCategory = TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK.equals(el.getCategory());
        if (availableCategory) {
            SendDigitalFeedbackDetailsInt details = SmartMapper.mapToClass(el.getDetails(), SendDigitalFeedbackDetailsInt.class);
            return recIndex.equals(details.getRecIndex());
        }
        return false;
    }
    
    public Instant getSchedulingDate(Instant notificationDate, Duration scheduleTime, String iun) {
        int hour = pnDeliveryPushConfigs.getTimeParams().getNotificationNonVisibilityTimeHours();
        int minute = pnDeliveryPushConfigs.getTimeParams().getNotificationNonVisibilityTimeMinutes();
        int second = 0;
        int nanoOfSecond = 0;

        log.debug("Start getSchedulingDate with notificationDate={} scheduleTime={} notificationNonVisibilityTime={}:{}:{}:{} - iun={}",
                notificationDate, scheduleTime, hour, minute, second, nanoOfSecond, iun);

        ZonedDateTime notificationDateTime = DateFormatUtils.parseInstantToZonedDateTime(notificationDate);
        ZonedDateTime notificationNonVisibilityDateTime = DateFormatUtils.setSpecificTimeToDate(notificationDateTime, hour, minute, second, nanoOfSecond);

        log.debug("Formatted notificationDateTime={} and notificationNonVisibilityDateTime={} - iun={}", notificationDateTime, notificationNonVisibilityDateTime, iun);

        if (notificationDateTime.isAfter(notificationNonVisibilityDateTime)){
            int daysToAddToScheduledTime = 1;
            scheduleTime = scheduleTime.plus(Duration.ofDays(daysToAddToScheduledTime));
            log.debug("NotificationDateTime is after notificationNonVisibilityDateTime, need to add {} day to schedulingTime. scheduleTime={} - iun={}", daysToAddToScheduledTime, scheduleTime, iun);
        } else {
            log.debug("NotificationDateTime is not after notificationNonVisibilityDateTime, don't need to add any day to schedulingTime. scheduleTime={} - iun={}", scheduleTime, iun);
        }

        Instant schedulingDate = notificationDate.plus(scheduleTime);

        log.info("Scheduling Date is {} - iun={}", schedulingDate, iun);

        return schedulingDate;
    }

    public void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
