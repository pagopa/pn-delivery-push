package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import lombok.*;

import java.time.Instant;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class GetAddressInfoDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private DigitalAddressSourceInt digitalAddressSource;
    private boolean isAvailable;
    private Instant attemptDate;
}
