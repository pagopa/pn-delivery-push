package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationValidationScheduler {
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(10);
    private final SchedulerService schedulerService;
    private final PnDeliveryPushConfigs configs;
    private final InstantNowSupplier instantNowSupplier;
    
    public void scheduleNotificationValidation(String iun) {
        Instant schedulingDate = Instant.now();

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(0)
                .build();

        log.info("Scheduling notification validation schedulingDate={} - iun={}", schedulingDate, iun);
        schedulerService.scheduleEvent(iun, schedulingDate, ActionType.NOTIFICATION_VALIDATION, details);
    }
    
    public void scheduleNotificationValidation(String iun, int retryAttempt) {
        log.info("Start NotificationValidationScheduler - iun={} retryAttempt={}", iun, retryAttempt);

        //Viene ottenuto l'array degli intervalli di retry
        Duration[] waitingTimeArray = configs.getValidationRetryIntervals();

        final int arrayLength = waitingTimeArray.length - 1;

        //Il retry attempt indica l'elemento dell'array da ottenere (dunque il prossimo intervallo di tempo di scheduling), 
        // se maggiore della grandezza dell'array, viene preso l'ultimo intervallo presente nell'array
        int waitingTimeIndex = Math.min(arrayLength, retryAttempt);
        Duration waitingTime = waitingTimeArray[waitingTimeIndex];

        //Se l'intervallo ottenuto è negativo significa che si vuole scehdulare all'infinito
        if(waitingTime.isNegative()){
            waitingTime = getWaitingTimeForInfiniteScheduling(waitingTimeArray, waitingTimeIndex);
        }

        scheduleEvent(iun, retryAttempt, waitingTime);
    }

    private void scheduleEvent(String iun, int retryAttempt, Duration waitingTime) {
        Instant schedulingDate = instantNowSupplier.get().plus(waitingTime);
        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(retryAttempt + 1)
                .build();

        log.info("Scheduling notification validation - iun={} schedulingDate={}", iun, schedulingDate);
        schedulerService.scheduleEvent(iun, schedulingDate, ActionType.NOTIFICATION_VALIDATION, details);
    }

    private Duration getWaitingTimeForInfiniteScheduling(Duration[] waitingTimeArray, int waitingTimeIndex) {
        Duration waitingTime;
        //Per ottenere il prossimo intervallo di scheduling, viene ottenuto l'intervallo di scheduling immediatamente precedente a quello negativo
        waitingTimeIndex = waitingTimeIndex -1;

        //Se presente un solo elemento nell'array ed è quello negativo (non dovrebbe accadere) il prossimo intervallo è definito di default
        if(waitingTimeIndex < 0){
            log.warn("Schedule time not defined in array");
            waitingTime = DEFAULT_INTERVAL;
        } else {
            waitingTime = waitingTimeArray[waitingTimeIndex];
        }
        return waitingTime;
    }
}
