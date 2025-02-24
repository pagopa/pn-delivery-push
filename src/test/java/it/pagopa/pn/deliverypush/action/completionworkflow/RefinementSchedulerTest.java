package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

class RefinementSchedulerTest {
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private SchedulerService scheduler;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private RefinementScheduler refinementScheduler;
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        refinementScheduler = new RefinementScheduler(
                timelineUtils,
                timelineService,
                scheduler,
                pnDeliveryPushConfigs
        );
    }
 
    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleDigitalRefinementSuccessNotViewedBeforeNonVisibilityTime() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTime("21:00");
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(10));

        times.setTimeToAddInNonVisibilityTimeCase(Duration.ofDays(0));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessDigitalRefinement());
        
        //WHEN
        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.SUCCESS);
        
        //THEN
        Mockito.verify(timelineUtils).buildScheduleRefinement(notification, recIndex, schedulingDateOk);
        
        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleDigitalRefinementNotSuccessViewedAfterNonVisibilityTime() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now().atZone(DateFormatUtils.italianZoneId)
                .withHour(21)
                .withMinute(1)
                .withSecond(0)
                .withNano(0)
                .toInstant();

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTime("21:00");
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofDays(10));

        times.setTimeToAddInNonVisibilityTimeCase(Duration.ofDays(1));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessDigitalRefinement());
        schedulingDateOk = schedulingDateOk.plus(times.getTimeToAddInNonVisibilityTimeCase());
        
        //WHEN
        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.SUCCESS);

        //THEN
        Mockito.verify(timelineUtils).buildScheduleRefinement(notification, recIndex, schedulingDateOk);

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleDigitalRefinementSuccessViewedBeforeNonVisibilityTime() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTime("21:00");
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(10));

        times.setTimeToAddInNonVisibilityTimeCase(Duration.ofDays(0));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessDigitalRefinement());

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        //WHEN
        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.SUCCESS);

        //THEN
        Mockito.verify(timelineUtils, Mockito.never()).buildScheduleRefinement(notification, recIndex, schedulingDateOk);
        Mockito.verify(scheduler, Mockito.never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.any(ActionType.class));
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleDigitalRefinementFailureViewedBeforeNonVisibilityTime() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setNotificationNonVisibilityTime("21:00");
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(20));

        times.setTimeToAddInNonVisibilityTimeCase(Duration.ofDays(0));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysFailureDigitalRefinement());
        
        //WHEN
        refinementScheduler.scheduleDigitalRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.FAILURE);

        //THEN
        Mockito.verify(timelineUtils).buildScheduleRefinement(notification, recIndex, schedulingDateOk);

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleAnalogRefinementSuccess() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(10));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessAnalogRefinement());
        
        //WHEN
        refinementScheduler.scheduleAnalogRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.SUCCESS);

        //THEN
        Mockito.verify(timelineUtils).buildScheduleRefinement(notification, recIndex, schedulingDateOk);

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleAnalogRefinementFailure() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(10));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysFailureAnalogRefinement());
        
        //WHEN
        refinementScheduler.scheduleAnalogRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.FAILURE);

        //THEN
        Mockito.verify(timelineUtils).buildScheduleRefinement(notification, recIndex, schedulingDateOk);

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }



    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleAnalogRefinementSuccessViewedAfterRefinement() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(10));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessAnalogRefinement());

        TimelineElementInternal viewedTimelineElement = new TimelineElementInternal();
        viewedTimelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        NotificationViewedCreationRequestDetailsInt notificationViewedCreationRequestDetailsInt = new NotificationViewedCreationRequestDetailsInt();
        notificationViewedCreationRequestDetailsInt.setEventTimestamp(Instant.now().plus(1l, ChronoUnit.DAYS));
        viewedTimelineElement.setDetails(notificationViewedCreationRequestDetailsInt);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        Mockito.when(timelineUtils.getNotificationViewCreationRequest(Mockito.anyString(), Mockito.anyInt())).thenReturn(Optional.of(viewedTimelineElement));

        //WHEN
        refinementScheduler.scheduleAnalogRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.SUCCESS);


        //THEN
        Mockito.verify(timelineUtils).buildScheduleRefinement(notification, recIndex, schedulingDateOk);

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleAnalogRefinementSuccessViewedBeforeRefinement() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(10));

        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessAnalogRefinement());

        TimelineElementInternal viewedTimelineElement = new TimelineElementInternal();
        viewedTimelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        NotificationViewedCreationRequestDetailsInt notificationViewedCreationRequestDetailsInt = new NotificationViewedCreationRequestDetailsInt();
        notificationViewedCreationRequestDetailsInt.setEventTimestamp(Instant.now().minus(1l, ChronoUnit.DAYS));
        viewedTimelineElement.setDetails(notificationViewedCreationRequestDetailsInt);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        Mockito.when(timelineUtils.getNotificationViewCreationRequest(Mockito.anyString(), Mockito.anyInt())).thenReturn(Optional.of(viewedTimelineElement));

        //WHEN
        refinementScheduler.scheduleAnalogRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.SUCCESS);


        //THEN
        Mockito.verify(timelineUtils, Mockito.never()).buildScheduleRefinement(notification, recIndex, schedulingDateOk);

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler, Mockito.never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

    }

}
