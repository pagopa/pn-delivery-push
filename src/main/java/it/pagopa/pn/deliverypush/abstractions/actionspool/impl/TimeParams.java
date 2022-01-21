package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import lombok.Data;

import java.time.Duration;

@Data
public class TimeParams {
    private Duration waitingResponseFromFirstAddress;
    private Duration secondAttemptWaitingTime;

    private Duration recipientViewMaxTimeForDigital;
    private Duration recipientViewMaxTimeForAnalog;
    private Duration refinementTimeForCompletelyUnreachable;

    private Duration waitingForNextAction;
    private Duration timeBetweenExtChReceptionAndMessageProcessed;
    private Duration intervalBetweenNotificationAndMessageReceived;

    private Duration waitingForReadCourtesyMessage;
    private Duration schedulingDaysSuccessDigitalRefinement;
    private Duration schedulingDaysFailureDigitalRefinement;
    private Duration schedulingDaysSuccessAnalogRefinement;
    private Duration schedulingDaysFailureAnalogRefinement;
    private Duration secondNotificationWorkflowWaitingTime;

}
