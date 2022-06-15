package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ScheduleDigitalWorkflowDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private LegalDigitalAddressInt digitalAddress;
    private DigitalAddressSourceInt digitalAddressSource;
    private Integer sentAttemptMade;
    private Instant lastAttemptDate;
}
