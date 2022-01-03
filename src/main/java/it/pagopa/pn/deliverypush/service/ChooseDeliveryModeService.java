package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;

;

public interface ChooseDeliveryModeService {
    void sendRequestForGetGeneralAddress(String iun, String taxId);

    void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource source, boolean isAvailable);
}
