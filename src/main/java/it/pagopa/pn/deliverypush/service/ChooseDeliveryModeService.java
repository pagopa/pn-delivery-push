package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;

public interface ChooseDeliveryModeService {
    void sendRequestForGetGeneralAddress(String iun, String taxId);

    void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource2 source, boolean isAvailable);
}
