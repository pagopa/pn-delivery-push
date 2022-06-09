package it.pagopa.pn.deliverypush.abstractions.webhookspool;

import lombok.*;
import org.springframework.util.StringUtils;

import java.time.Instant;

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
        if (StringUtils.hasText(eventId))
            return eventId;
        else if (timestamp != null)
            return timestamp + "_" + timelineId;
        else
            return null;
    }

}
