package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;

public interface DigitalAddressRelatedTimelineElement extends ConfidentialInformationTimelineElement{
    LegalDigitalAddressInt getDigitalAddress();
    void setDigitalAddress(LegalDigitalAddressInt digitalAddressInt);
}
