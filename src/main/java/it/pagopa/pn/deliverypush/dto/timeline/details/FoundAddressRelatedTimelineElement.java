package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

public interface FoundAddressRelatedTimelineElement extends ConfidentialInformationTimelineElement{
    PhysicalAddressInt getFoundAddress();
    void setFoundAddress(PhysicalAddressInt physicalAddressInt);
}
