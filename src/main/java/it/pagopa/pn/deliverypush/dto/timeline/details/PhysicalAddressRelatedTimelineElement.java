package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

public interface PhysicalAddressRelatedTimelineElement extends ConfidentialInformationTimelineElement{
    PhysicalAddressInt getPhysicalAddress();
    void setPhysicalAddress(PhysicalAddressInt digitalAddressInt);
}
