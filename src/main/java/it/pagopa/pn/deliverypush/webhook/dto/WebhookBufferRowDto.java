package it.pagopa.pn.deliverypush.webhook.dto;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.time.Instant;

@Value
@Builder( toBuilder = true )
@ToString
public class WebhookBufferRowDto {
    private final String senderId;
    private final Instant statusChangeTime;
    private final String iun;
    private final String notificationElement;
}
