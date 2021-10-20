package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import lombok.Data;

import java.time.Duration;

@Data
public class TimeParams {

    private Duration waitingResponseFromFirstAddress;
    private Duration secondAttemptWaitingTime;

    private Duration recipientViewMaxTimeForDigital;
    private Duration recipientViewMaxTimeForAnalog;

    private Duration waitingForNextAction;
    private Duration timeBetweenExtChReceptionAndMessageProcessed;
    private Duration intervalBetweenNotificationAndMessageReceived;

}
