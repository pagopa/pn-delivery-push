package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;

import java.util.Collections;
import java.util.List;

public class AddressBookEntryTestBuilder {
    private List<DigitalAddress> courtesyAddresses;
    private DigitalAddresses digitalAddresses;
    private String taxId;

    private AddressBookEntryTestBuilder() {
        this.courtesyAddresses = Collections.emptyList();
        this.digitalAddresses = DigitalAddresses.builder().build();
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
        this.digitalAddresses = this.digitalAddresses.toBuilder()
                .platform(platformAddress)
                .build();
        return this;
    }

    public AddressBookEntry build() {
        return AddressBookEntry.builder()
                .taxId(taxId)
                .courtesyAddresses(courtesyAddresses)
                .digitalAddresses(digitalAddresses)
                .build();
    }
}
