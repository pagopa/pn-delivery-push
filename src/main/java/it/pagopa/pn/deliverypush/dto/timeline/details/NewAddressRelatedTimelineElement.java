package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

public interface NewAddressRelatedTimelineElement {
    PhysicalAddressInt getNewAddress();
    void setNewAddress(PhysicalAddressInt digitalAddressInt);
}
