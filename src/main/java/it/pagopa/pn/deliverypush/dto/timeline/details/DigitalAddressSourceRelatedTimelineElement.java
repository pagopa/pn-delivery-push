package it.pagopa.pn.deliverypush.dto.timeline.details;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;

public interface DigitalAddressSourceRelatedTimelineElement  extends RecipientRelatedTimelineElementDetails {
    DigitalAddressSourceInt getDigitalAddressSource();
    void setDigitalAddressSource(DigitalAddressSourceInt digitalAddressInt);
}
