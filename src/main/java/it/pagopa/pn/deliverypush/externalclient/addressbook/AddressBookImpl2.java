package it.pagopa.pn.deliverypush.externalclient.addressbook;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AddressBookImpl2 implements AddressBook {
    private final it.pagopa.pn.commons.pnclients.addressbook.AddressBook legacy;

    public AddressBookImpl2(it.pagopa.pn.commons.pnclients.addressbook.AddressBook legacy) {
        this.legacy = legacy;
    }
    
    @Override
    public Optional<AddressBookEntry> getAddresses(String taxId, NotificationSenderInt sender) {
        return legacy.getAddresses(taxId).map((it.pagopa.pn.api.dto.addressbook.AddressBookEntry legacyDto )-> 
                AddressBookEntry.builder()
                .courtesyAddresses(legacyDto.getCourtesyAddresses().stream().map(
                        legacyAddress -> DigitalAddress.builder()
                                .type(DigitalAddress.TypeEnum.valueOf(legacyAddress.getType().name()))
                                .address(legacyAddress.getAddress())
                                .build()
                        ).collect(Collectors.toList())
                )
                .platformDigitalAddress(
                        DigitalAddress.builder()
                                .type(DigitalAddress.TypeEnum.valueOf(legacyDto.getDigitalAddresses().getPlatform().getType().name()))
                                .address(legacyDto.getDigitalAddresses().getPlatform().getAddress())
                                .build()
                        )
                .residentialAddress(
                        PhysicalAddress.builder()
                                .zip(legacyDto.getResidentialAddress().getZip())
                                .province(legacyDto.getResidentialAddress().getProvince())
                                .municipality(legacyDto.getResidentialAddress().getMunicipality())
                                .addressDetails(legacyDto.getResidentialAddress().getAddressDetails())
                                .address(legacyDto.getResidentialAddress().getAddress())
                                .at(legacyDto.getResidentialAddress().getAt())
                                .foreignState(legacyDto.getResidentialAddress().getForeignState())
                                .build()
                )
                .taxId(legacyDto.getTaxId())
                .build());
    }
}
