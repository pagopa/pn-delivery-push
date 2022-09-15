package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class CompletionWorkflowUtilsTest {
    private CompletionWorkflowUtils completionWorkflowUtils;

    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private TimelineService timelineService;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        completionWorkflowUtils = new CompletionWorkflowUtils(pnDeliveryPushConfigs, timelineService, saveLegalFactsService, notificationUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generatePecDeliveryWorkflowLegalFact() {
        //GIVEN
        
        int recIndex = 0;
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("taxId")
                .withInternalId("ANON_"+"taxId")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        
        Set<TimelineElementInternal> timeline = getTimeline(notification.getIun(), recIndex);
        
        Mockito.when(timelineService.getTimeline(Mockito.anyString())).thenReturn(timeline);
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        
        //WHEN
        completionWorkflowUtils.generatePecDeliveryWorkflowLegalFact(notification, recIndex);
        
        //THEN
        Mockito.verify(saveLegalFactsService).savePecDeliveryWorkflowLegalFact(Mockito.anyList(), Mockito.eq(notification), Mockito.eq(recipient));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getSchedulingDateBeforeNotificationVisibilityTime() {
        //GIVEN
        Instant notificationDate = Instant.now();

        notificationDate = notificationDate.atZone(ZoneOffset.UTC)
                .withHour(13)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant();

        Duration scheduleTime = Duration.ofDays(10);

        int hour= 21;
        int minute= 0;

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTimeHours(hour);
        times.setNotificationNonVisibilityTimeMinutes(minute);
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        
        //WHEN
        Instant schedulingDate = completionWorkflowUtils.getSchedulingDate(notificationDate, scheduleTime, "iun");

        //THEN
        Assertions.assertEquals(notificationDate.plus(scheduleTime) , schedulingDate);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getSchedulingDateAfterNotificationVisibilityTime() {
        //GIVEN
        Instant notificationDate = Instant.now();

        notificationDate = notificationDate.atZone(ZoneOffset.UTC)
                .withHour(19)
                .withMinute(1)
                .withSecond(0)
                .withNano(0)
                .toInstant();

        Duration scheduleTime = Duration.ofDays(10);

        int hour= 21;
        int minute= 0;

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTimeHours(hour);
        times.setNotificationNonVisibilityTimeMinutes(minute);
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        //WHEN
        Instant schedulingDate = completionWorkflowUtils.getSchedulingDate(notificationDate, scheduleTime, "iun");
        
        //THEN
        Duration scheduledTimeExpected = scheduleTime.plus(Duration.ofDays(1));
        Assertions.assertEquals(notificationDate.plus(scheduledTimeExpected) , schedulingDate);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void addTimelineElement() {
        //GIVEN
        TimelineElementInternal timelineElementInternal = getSendDigitalFeedbackDetailsTimelineElement("iun", 0);

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("taxId")
                .withInternalId("ANON_"+"taxId")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress("_Via Nuova")
                                .build()
                )
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        
        //WHEN
        completionWorkflowUtils.addTimelineElement(timelineElementInternal, notification);
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
    }


    private Set<TimelineElementInternal> getTimeline(String iun, int recIndex){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        TimelineElementInternal timelineElementInternal = getSendDigitalFeedbackDetailsTimelineElement(iun, recIndex);
        timelineElementList.add(timelineElementInternal);
        return new HashSet<>(timelineElementList);
    }

    private TimelineElementInternal getSendDigitalFeedbackDetailsTimelineElement(String iun, int recIndex) {
        SendDigitalFeedbackDetailsInt details =  SendDigitalFeedbackDetailsInt.builder()
                .recIndex(recIndex)
                .build();
        return TimelineElementInternal.builder()
                .elementId("elementId")
                .iun(iun)
                .details( details )
                .build();
    }
}