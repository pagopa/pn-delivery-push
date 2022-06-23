package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ExtChannelAnalogSentResponseInt {
    private String requestId;
    private String iun;
    private List<AttachmentDetailsInt> attachments = null;
    private String statusCode;
    private Instant statusDateTime;
    private String statusDescription;
    private PhysicalAddressInt discoveredAddress;
    private String deliveryFailureCause;

}
