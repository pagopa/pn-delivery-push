package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool;

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

    private String eventId;

    private String paId;

    private String iun;

    private Integer delay;

    private Instant timestamp;

    private String timelineId;

    private String oldStatus;

    private String newStatus;

    private String timelineEventCategory;

    private WebhookEventType type;
}
