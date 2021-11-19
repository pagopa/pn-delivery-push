package it.pagopa.pn.deliverypush.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Builder
@ToString
public class WebhookOutputDto {
    private final String senderId;
    private final String statusChangeTime;
    private final String iun;
    private final String notificationElement;
}
