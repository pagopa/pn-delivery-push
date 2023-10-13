package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;

import static it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationScheduler.DEFAULT_INTERVAL;

class NotificationValidationSchedulerTest {
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private PnDeliveryPushConfigs configs;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;

    private NotificationValidationScheduler notificationValidationScheduler;

    @BeforeEach
    public void setup() {
        notificationValidationScheduler = new NotificationValidationScheduler(schedulerService, configs, instantNowSupplier, timelineService, timelineUtils);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void scheduleNotificationValidation() {
        //GIVEN
        String iun = "test";
        //WHEN
        notificationValidationScheduler.scheduleNotificationValidation(iun);
        //THEN
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(iun), Mockito.any(Instant.class), Mockito.eq(ActionType.NOTIFICATION_VALIDATION), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void testScheduleNotificationValidation() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();

        Duration [] intervalsDuration = { Duration.ofSeconds(2), Duration.ofSeconds(3) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);
        
        //WHEN
        int retryAttempt = 0;
        notificationValidationScheduler.scheduleNotificationValidation(notification, retryAttempt, null, Instant.now());
        
        //THEN
        Instant schedulingDate = now.plus(intervalsDuration[retryAttempt]);
        
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(schedulingDate), Mockito.eq(ActionType.NOTIFICATION_VALIDATION), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void testScheduleNotificationValidationInfinite() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        Duration [] intervalsDuration = { Duration.ofSeconds(2), Duration.ofSeconds(3), Duration.ofSeconds(-1) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);

        //WHEN
        int retryAttempt = 2;
        notificationValidationScheduler.scheduleNotificationValidation(notification, retryAttempt, null, Instant.now());

        //THEN
        Instant schedulingDate = now.plus(intervalsDuration[retryAttempt - 1]);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(schedulingDate), Mockito.eq(ActionType.NOTIFICATION_VALIDATION), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void testScheduleNotificationValidationInfiniteOneInterval() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        Duration [] intervalsDuration = { Duration.ofSeconds(-1) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);

        //WHEN
        int retryAttempt = 2;
        notificationValidationScheduler.scheduleNotificationValidation(notification, retryAttempt,null, Instant.now());

        //THEN
        Instant schedulingDate = now.plus(DEFAULT_INTERVAL);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(schedulingDate), Mockito.eq(ActionType.NOTIFICATION_VALIDATION), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void testScheduleNotificationValidationRefused() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();

        Duration [] intervalsDuration = { Duration.ofSeconds(2), Duration.ofSeconds(3) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        //WHEN
        int retryAttempt = 2;
        notificationValidationScheduler.scheduleNotificationValidation(notification, retryAttempt,null, Instant.now());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void testScheduleNotificationValidationNotFoundRefused() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();

        Duration [] intervalsDuration = { Duration.ofSeconds(2), Duration.ofSeconds(3) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        //WHEN
        int retryAttempt = 2;
        PnValidationFileNotFoundException ex = new PnValidationFileNotFoundException( "file non trovato", new Throwable() );
        notificationValidationScheduler.scheduleNotificationValidation(notification, retryAttempt, ex, Instant.now());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
    }
}