package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Action {

    private String iun;

    private String actionId;

    private Instant notBefore;

    private ActionType type;

    // Required and used for SEND_PEC and RECEIVE_PEC ActionType
    private Integer recipientIndex;

    private String timelineId;

    private String timeslot;
}
