package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendDigitalFeedbackDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private ResponseStatusInt responseStatus;
    private Instant notificationDate;
    private List<String> errors;
}
