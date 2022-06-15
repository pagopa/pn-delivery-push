package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendCourtesyMessageDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private CourtesyDigitalAddressInt digitalAddress;
    private Instant sendDate;
}
