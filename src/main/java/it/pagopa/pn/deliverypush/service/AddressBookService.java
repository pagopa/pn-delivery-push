package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;

import java.util.List;
import java.util.Optional;

public interface AddressBookService {
    Optional<DigitalAddress> getPlatformAddresses(String taxId, String senderId);

    Optional<List<DigitalAddress>> getCourtesyAddress(String taxId, String senderId);

    Optional<PhysicalAddress> getResidentialAddress(String taxId, String senderId);
}
