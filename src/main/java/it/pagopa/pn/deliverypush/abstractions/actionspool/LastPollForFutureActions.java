package it.pagopa.pn.deliverypush.abstractions.actionspool;


import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
public class LastPollForFutureActions {

    private Long lastPollKey;

    private Instant lastPollExecuted;

}
