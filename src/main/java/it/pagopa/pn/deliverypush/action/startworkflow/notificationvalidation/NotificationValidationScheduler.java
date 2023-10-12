package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationValidationScheduler {
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(10);
    private final SchedulerService schedulerService;
    private final PnDeliveryPushConfigs configs;
    private final InstantNowSupplier instantNowSupplier;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public void scheduleNotificationValidation(String iun) {
        Instant schedulingDate = Instant.now();

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(0)
                .startWorkflowTime(Instant.now())
                .build();

        log.info("Scheduling notification validation schedulingDate={} - iun={}", schedulingDate, iun);
        schedulerService.scheduleEvent(iun, schedulingDate, ActionType.NOTIFICATION_VALIDATION, details);
    }
    
    public void scheduleNotificationValidation(NotificationInt notification, int retryAttempt, Exception ex, Instant startWorkflowTime) {
        String iun = notification.getIun();
        log.info("Start NotificationValidationScheduler - iun={} retryAttempt={}", iun, retryAttempt);

        //Viene ottenuto l'array degli intervalli di retry
        Duration[] waitingTimeArray = configs.getValidationRetryIntervals();

        final int arrayLength = waitingTimeArray.length - 1;

        //Il retry attempt indica l'elemento dell'array da ottenere (dunque il prossimo intervallo di tempo di scheduling), 
        // se maggiore della grandezza dell'array, viene preso l'ultimo intervallo presente nell'array
        int waitingTimeIndex = Math.min(arrayLength, retryAttempt);
        Duration waitingTime = waitingTimeArray[waitingTimeIndex];
        
        if(waitingTime.isNegative()){
            //Se l'intervallo ottenuto è negativo significa che si vuole scehdulare all'infinito, anche se il retryAttempt è maggiore della grandezza array
            //dovendo schedulare all'infinito la notifica non va in rifiutata
            calculateWaitTimeAndScheduleEvent(retryAttempt, iun, waitingTimeArray, waitingTimeIndex, startWorkflowTime);
        } else {
            //Se il waitingTime non è negativo, significa che non devo schedulare all'infinito
            log.debug("WaitingTime is not negative - iun={} retryAttempt={}", iun, retryAttempt);
            if(retryAttempt > arrayLength){
                /*Se retryAttempt > arrayLength ho terminato i ritentativi possibili porto la notifica in rifiutata (l'unico caso in cui devo
                rischedulare anche in caso di retryAttempt > arrayLength è se devo scehdulare all'infinito, ma è stato escluso nello step precedente */
                log.debug("retryAttempt={} is greater then arrayLength={} - need to refuse notification - iun={}", retryAttempt, arrayLength, iun);
                handleValidationError(notification, ex);
            } else {
                //altrimenti schedulo il nuovo tentativo
                log.debug("Need to schedule new attempt - iun={}", iun);
                scheduleEvent(iun, retryAttempt, waitingTime, startWorkflowTime);
            }
        }
    }

    private void calculateWaitTimeAndScheduleEvent(int retryAttempt, String iun, Duration[] waitingTimeArray, int waitingTimeIndex, Instant startWorkflowTime) {
        log.debug("WaitingTime is negative, infinite scheduling- iun={} retryAttempt={}", iun, retryAttempt);
        Duration waitingTime = getWaitingTimeForInfiniteScheduling(waitingTimeArray, waitingTimeIndex);
        scheduleEvent(iun, retryAttempt, waitingTime, startWorkflowTime);
    }

    private void scheduleEvent(String iun, int retryAttempt, Duration waitingTime, Instant startWorkflowTime) {
        
        Instant schedulingDate = instantNowSupplier.get().plus(waitingTime);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(retryAttempt + 1)
                .startWorkflowTime(startWorkflowTime)
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

    private void handleValidationError(NotificationInt notification, Exception ex) {
        List<NotificationRefusedErrorInt> errors = new ArrayList<>();
        NotificationRefusedErrorInt notificationRefusedError;

        if(Objects.nonNull(ex) && ex instanceof PnValidationFileNotFoundException) {
            notificationRefusedError = NotificationRefusedErrorInt.builder()
                    .errorCode("FILE_NOTFOUND")
                    .detail("Allegati non trovati")
                    .build();
        } else {
            notificationRefusedError = NotificationRefusedErrorInt.builder()
                    .errorCode("SERVICE_UNAVAILABLE")
                    .detail("Servizio non disponibile")
                    .build();
        }
        errors.add(notificationRefusedError);
        
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
