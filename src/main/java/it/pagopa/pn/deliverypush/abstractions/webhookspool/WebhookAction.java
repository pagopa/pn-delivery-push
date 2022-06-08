package it.pagopa.pn.deliverypush.abstractions.webhookspool;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class WebhookAction {

    private String streamId;

    private String iun;

    private String requestId;

    private Instant timestamp;

    private String eventId;

    private String newStatus;

    private String timelineEventCategory;

}
