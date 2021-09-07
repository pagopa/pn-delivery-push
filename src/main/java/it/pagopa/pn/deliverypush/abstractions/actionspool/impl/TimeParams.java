package it.pagopa.pn.deliverypush.abstractions.actionspool.impl;

import lombok.Data;

import java.time.Duration;

@Data
public class TimeParams {

    private String param;
    private Duration secondAttemptWaitingTime;
    private Duration processingTimeToRecipient;
    private Duration waitingResponseFromFirstAddress;
    private Duration waitingForNextAction;
    private Duration timeBetweenExtChReceptionAndMessageProcessed;
    private Duration intervalBetweenNotificationAndMessageReceived;

}