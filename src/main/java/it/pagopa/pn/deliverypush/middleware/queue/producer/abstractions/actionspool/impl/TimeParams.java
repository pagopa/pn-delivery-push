package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import lombok.Data;

import java.time.Duration;

@Data
public class TimeParams {
    private Duration waitingForReadCourtesyMessage;
    private Duration schedulingDaysSuccessDigitalRefinement;
    private Duration schedulingDaysFailureDigitalRefinement;
    private Duration schedulingDaysSuccessAnalogRefinement;
    private Duration schedulingDaysFailureAnalogRefinement;
    private Duration secondNotificationWorkflowWaitingTime;
    private int notificationNonVisibilityTimeHours;
    private int notificationNonVisibilityTimeMinutes;
}
