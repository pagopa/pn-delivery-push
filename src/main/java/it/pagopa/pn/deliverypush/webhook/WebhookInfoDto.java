package it.pagopa.pn.deliverypush.webhook;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.time.Instant;

@Value
@Builder( toBuilder = true )
@ToString
public class WebhookInfoDto {
    private final String paId;
    private final String url;
    private final Instant startFrom;
    private final boolean active;
}
