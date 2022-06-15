package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder(toBuilder = true)
@ToString
public class SendAnalogFeedbackDetailsInt implements RecipientRelatedTimelineElementDetails {
    private Integer recIndex;
    private PhysicalAddressInt physicalAddress;
    private ServiceLevelInt serviceLevel;
    private Integer sentAttemptMade;
    private Boolean investigation;
    private PhysicalAddressInt newAddress;
    private List<String> errors = null;
}
