package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;

import java.util.List;
import java.util.Optional;

public interface AddressBookService {
    Optional<LegalDigitalAddressInt> getPlatformAddresses(String taxId, String senderId);

    Optional<List<CourtesyDigitalAddressInt>> getCourtesyAddress(String taxId, String senderId);

    Optional<PhysicalAddress> getResidentialAddress(String taxId, String senderId);
}
