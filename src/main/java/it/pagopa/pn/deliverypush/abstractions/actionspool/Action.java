package it.pagopa.pn.deliverypush.abstractions.actionspool;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
public class Action {

    private String iun;

    private String actionId;

    private Instant notBefore;

    private ActionType type;

    private Integer recipientIndex;

    private DigitalAddressSource digitalAddressSource;

    private Integer retryNumber;

    private PnExtChnProgressStatus responseStatus;

}
