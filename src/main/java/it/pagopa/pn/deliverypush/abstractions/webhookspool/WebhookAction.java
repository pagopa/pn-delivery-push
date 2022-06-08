package it.pagopa.pn.deliverypush.abstractions.webhookspool;

import lombok.*;

import java.time.Instant;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class WebhookAction {

    private String streamId;

    private String eventId;

    private String iun;

    private Instant notBefore;

    private String requestId;

    private Instant timestamp;

    private String timelineId;

    private String newStatus;

    private String timelineEventCategory;

    private WebhookEventType type;

    public String getEventId()
    {
        return Objects.requireNonNullElseGet(eventId, () -> timestamp.toString() + "_" + timelineId);
    }

}
