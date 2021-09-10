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

    // Required and used for SEND_PEC and RECEIVE_PEC ActionType
    private Integer recipientIndex;

    // Required and used for SEND_PEC and RECEIVE_PEC ActionType
    private DigitalAddressSource digitalAddressSource;

    // Required and used for SEND_PEC and RECEIVE_PEC ActionType
    private Integer retryNumber;

    // Required and used only for RECEIVE_PEC ActionType
    private PnExtChnProgressStatus responseStatus;

}
