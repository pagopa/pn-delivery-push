package it.pagopa.pn.deliverypush.action2.it.utils;
import it.pagopa.pn.deliverypush.external.AddressBookEntry;

import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;

import java.util.Collections;
import java.util.List;

public class AddressBookEntryTestBuilder {
    private List<DigitalAddress> courtesyAddresses;
    private String taxId;
    private DigitalAddress platformAddress;
    
    private AddressBookEntryTestBuilder() {
        this.courtesyAddresses = Collections.emptyList();
    }

    public static AddressBookEntryTestBuilder builder() {
        return new AddressBookEntryTestBuilder();
    }

    public AddressBookEntryTestBuilder withTaxId(String taxId) {
        this.taxId = taxId;
        return this;
    }

    public AddressBookEntryTestBuilder withCourtesyAddress(String courtesyAddress) {
        this.courtesyAddresses = Collections.singletonList((DigitalAddress.builder()
                .address(courtesyAddress)
                .type(DigitalAddressType.PEC)
                .build()));
        return this;
    }

    public AddressBookEntryTestBuilder withPlatformAddress(DigitalAddress platformAddress) {
        this.platformAddress = platformAddress;
        return this;
    }

    public AddressBookEntry build() {
        return AddressBookEntry.builder()
                .taxId(taxId)
                .courtesyAddresses(courtesyAddresses)
                .platformDigitalAddress(platformAddress)
                .build();
    }
}
