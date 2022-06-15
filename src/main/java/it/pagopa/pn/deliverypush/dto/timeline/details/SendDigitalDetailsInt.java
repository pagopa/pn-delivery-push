package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendDigitalDetailsInt implements RecipientRelatedTimelineElementDetails, DigitalAddressRelatedTimelineElement {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
    private Integer retryNumber;
    private DownstreamIdInt downstreamId;
}
