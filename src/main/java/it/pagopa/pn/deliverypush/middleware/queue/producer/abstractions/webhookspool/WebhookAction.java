package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool;

import lombok.*;

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

    private String timelineId;

    private WebhookEventType type;
}
