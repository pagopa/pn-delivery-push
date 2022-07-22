package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendDigitalProgressDetailsInt implements RecipientRelatedTimelineElementDetails, DigitalAddressRelatedTimelineElement {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private Instant notificationDate;
    private List<SendingReceipt> sendingReceipts;
    
    //Non mostrati al FE
    private DigitalAddressSourceInt source;
    private Integer retryNumber;
}
