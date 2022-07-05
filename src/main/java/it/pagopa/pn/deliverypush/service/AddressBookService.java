package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;

import java.util.List;
import java.util.Optional;

public interface AddressBookService {
    Optional<LegalDigitalAddressInt> getPlatformAddresses(String taxIdAnon, String senderId);

    Optional<List<CourtesyDigitalAddressInt>> getCourtesyAddress(String taxIdAnon, String senderId);

    Optional<PhysicalAddressInt> getResidentialAddress(String taxId, String senderId);
}
