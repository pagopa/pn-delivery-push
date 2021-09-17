package it.pagopa.pn.deliverypush.middleware.actiondao.cassandra;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("last_poll_for_future_actions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
public class LastPollForFutureActionsEntity {

    @Id
    private Long lastPollKey;

    private Instant lastPollExecuted;

}
