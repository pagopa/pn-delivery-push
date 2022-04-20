package it.pagopa.pn.deliverypush.abstractions.actionspool;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import lombok.*;

import java.time.Instant;
import java.util.List;

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

    // TaxId Recipient
    private String taxId;

    // Required and used for SEND_PEC and RECEIVE_PEC ActionType
    private DigitalAddressSource digitalAddressSource;

    // Required and used for SEND_PEC and RECEIVE_PEC ActionType
    private Integer retryNumber;

    // Required and used only for RECEIVE_PEC ActionType
    private PnExtChnProgressStatus responseStatus;

    // Optional and used only for RECEIVE_PAPER ActionType
    private PhysicalAddress newPhysicalAddress; //FIXME verificare per privacy

    // Required and used only for RECEIVER_PAPER ActionType
    private List<String> attachmentKeys;

}
