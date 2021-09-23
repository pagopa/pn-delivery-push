package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder( toBuilder = true )
public class WebhookBufferRowDto {
    private final String senderId;
    private final Instant statusChangeTime;
    private final String iun;
    private final NotificationStatus status;
}
