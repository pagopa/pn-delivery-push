package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;

public interface CourtesyAddressRelatedTimelineElement {
    CourtesyDigitalAddressInt getDigitalAddress();
    void setDigitalAddress(CourtesyDigitalAddressInt digitalAddressInt);
}
