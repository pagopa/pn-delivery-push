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
    private Boolean isAvailable;
    private Instant attemptDate;

    public String toLog() {
        return String.format(
                "recIndex=%d digitalAddressSource=%s isAvailable=%s",
                recIndex,
                digitalAddressSource,
                isAvailable
        );
    }
}
