package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;

public interface DigitalSendTimelineElementDetails extends DigitalAddressRelatedTimelineElement, RecipientRelatedTimelineElementDetails {

    int getRecIndex();

    LegalDigitalAddressInt getDigitalAddress();
    void setDigitalAddress(LegalDigitalAddressInt digitalAddressInt);

    DigitalAddressSourceInt getDigitalAddressSource();
    void setDigitalAddressSource(DigitalAddressSourceInt digitalAddressSource);

    Integer getRetryNumber();
    void setRetryNumber(Integer retryNumber);
}
