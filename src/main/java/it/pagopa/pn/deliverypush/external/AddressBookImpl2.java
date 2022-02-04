package it.pagopa.pn.deliverypush.external;

import it.pagopa.pn.api.dto.notification.NotificationSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AddressBookImpl2 implements AddressBook {
    private final it.pagopa.pn.commons.pnclients.addressbook.AddressBook legacy;

    public AddressBookImpl2(it.pagopa.pn.commons.pnclients.addressbook.AddressBook legacy) {
        this.legacy = legacy;
    }
    
    @Override
    public Optional<AddressBookEntry> getAddresses(String taxId, NotificationSender sender) {
        return legacy.getAddresses(taxId).map((it.pagopa.pn.api.dto.addressbook.AddressBookEntry legacyDto )-> AddressBookEntry.builder()
                .courtesyAddresses(legacyDto.getCourtesyAddresses())
                .platformDigitalAddress(legacyDto.getDigitalAddresses().getPlatform())
                .residentialAddress(legacyDto.getResidentialAddress())
                .taxId(legacyDto.getTaxId())
                .build());
    }
}
