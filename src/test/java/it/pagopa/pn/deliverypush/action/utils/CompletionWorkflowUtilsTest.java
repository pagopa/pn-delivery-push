package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
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
import java.util.*;

import static it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK;

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
    void generatePecDeliveryWorkflowLegalFactWithFeedback() {
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
        
        List<TimelineElementInternal> timeline = getTimeline(notification.getIun(), recIndex);
        
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(new HashSet<>(timeline));
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        
        //WHEN
        EndWorkflowStatus status = EndWorkflowStatus.SUCCESS;
        Instant completionWorkflowDate = Instant.now();
        completionWorkflowUtils.generatePecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);

        TimelineElementInternal timelineElementInternal = timeline.get(0);
        SendDigitalFeedbackDetailsInt details = (SendDigitalFeedbackDetailsInt) timelineElementInternal.getDetails();
        
        //THEN
        Mockito.verify(saveLegalFactsService).savePecDeliveryWorkflowLegalFact(
                Mockito.eq(Collections.singletonList(details)),
                Mockito.eq(notification),
                Mockito.eq(recipient),
                Mockito.eq(status),
                Mockito.eq(completionWorkflowDate)
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void generatePecDeliveryWorkflowLegalFactWithFeedbackAndRegisteredLetter() {
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

        List<TimelineElementInternal> timeline = getTimelineWithRegisteredLetter(notification.getIun(), recIndex);

        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(new HashSet<>(timeline));
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        //WHEN
        EndWorkflowStatus status = EndWorkflowStatus.SUCCESS;
        Instant completionWorkflowDate = Instant.now();
        completionWorkflowUtils.generatePecDeliveryWorkflowLegalFact(notification, recIndex, status, completionWorkflowDate);

        TimelineElementInternal timelineElementInternal = timeline.get(0);
        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetailsInt = (SendDigitalFeedbackDetailsInt) timelineElementInternal.getDetails();

        TimelineElementInternal timelineElementInternal2 = timeline.get(1);
        SimpleRegisteredLetterDetailsInt registeredLetterDetails = (SimpleRegisteredLetterDetailsInt) timelineElementInternal2.getDetails();

        //THEN
        Mockito.verify(saveLegalFactsService).savePecDeliveryWorkflowLegalFact(
                Collections.singletonList(sendDigitalFeedbackDetailsInt),
                notification,
                recipient,
                status,
                completionWorkflowDate
        );
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

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTime("21:00");
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

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTime("21:00");
        times.setTimeToAddInNonVisibilityTimeCase(Duration.ofDays(1));
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


    private List<TimelineElementInternal> getTimeline(String iun, int recIndex){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        TimelineElementInternal timelineElementInternal = getSendDigitalFeedbackDetailsTimelineElement(iun, recIndex);
        timelineElementList.add(timelineElementInternal);
        return timelineElementList;
    }

    private TimelineElementInternal getSendDigitalFeedbackDetailsTimelineElement(String iun, int recIndex) {
        SendDigitalFeedbackDetailsInt details =  SendDigitalFeedbackDetailsInt.builder()
                .recIndex(recIndex)
                .build();
        return TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .elementId("elementId")
                .category(SEND_DIGITAL_FEEDBACK)
                .iun(iun)
                .details( details )
                .build();
    }

    private List<TimelineElementInternal> getTimelineWithRegisteredLetter(String iun, int recIndex){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        timelineElementList.add(getSendDigitalFeedbackDetailsTimelineElement(iun, recIndex));
        timelineElementList.add(getRegisteredLetterDetailsTimelineElement(iun, recIndex));
        return timelineElementList;
    }
    
    private TimelineElementInternal getRegisteredLetterDetailsTimelineElement(String iun, int recIndex) {
        SimpleRegisteredLetterDetailsInt details =  SimpleRegisteredLetterDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .at("001")
                                .address("002")
                                .addressDetails("003")
                                .zip("004")
                                .municipality("005")
                                .province("007")
                                .foreignState("008").build()
                )
                .build();
        
        return TimelineElementInternal.builder()
                .elementId("elementId2")
                .timestamp(Instant.now())
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .iun(iun)
                .details( details )
                .build();
    }
}