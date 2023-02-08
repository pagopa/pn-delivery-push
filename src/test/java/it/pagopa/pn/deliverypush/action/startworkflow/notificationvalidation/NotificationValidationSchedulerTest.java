package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
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
    
    private NotificationValidationScheduler notificationValidationScheduler;

    @BeforeEach
    public void setup() {
        notificationValidationScheduler = new NotificationValidationScheduler(schedulerService, configs, instantNowSupplier);
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
        String iun = "test";
        Duration [] intervalsDuration = { Duration.ofSeconds(2), Duration.ofSeconds(3) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);
        
        //WHEN
        int retryAttempt = 0;
        notificationValidationScheduler.scheduleNotificationValidation(iun, retryAttempt);
        
        //THEN
        Instant schedulingDate = now.plus(intervalsDuration[retryAttempt]);
        
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(iun), Mockito.eq(schedulingDate), Mockito.eq(ActionType.NOTIFICATION_VALIDATION), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void testScheduleNotificationValidationInfinite() {
        //GIVEN
        String iun = "test";
        Duration [] intervalsDuration = { Duration.ofSeconds(2), Duration.ofSeconds(3), Duration.ofSeconds(-1) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);

        //WHEN
        int retryAttempt = 2;
        notificationValidationScheduler.scheduleNotificationValidation(iun, retryAttempt);

        //THEN
        Instant schedulingDate = now.plus(intervalsDuration[retryAttempt - 1]);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(iun), Mockito.eq(schedulingDate), Mockito.eq(ActionType.NOTIFICATION_VALIDATION), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void testScheduleNotificationValidationInfiniteOneInterval() {
        //GIVEN
        String iun = "test";
        Duration [] intervalsDuration = { Duration.ofSeconds(-1) };
        Mockito.when(configs.getValidationRetryIntervals()).thenReturn(intervalsDuration);

        Instant now = Instant.now();
        Mockito.when(instantNowSupplier.get()).thenReturn(now);

        //WHEN
        int retryAttempt = 2;
        notificationValidationScheduler.scheduleNotificationValidation(iun, retryAttempt);

        //THEN
        Instant schedulingDate = now.plus(DEFAULT_INTERVAL);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(iun), Mockito.eq(schedulingDate), Mockito.eq(ActionType.NOTIFICATION_VALIDATION), Mockito.any());
    }
}