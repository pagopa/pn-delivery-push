package it.pagopa.pn.deliverypush.action2.it.testbean;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddressBookTest implements AddressBook {
    private final List<AddressBookEntry> addressBook;

    public AddressBookTest(List<AddressBookEntry> addressBook) {
        this.addressBook = addressBook;

        List<DigitalAddress> cuortesyAddress = new ArrayList<>();
        cuortesyAddress.add(DigitalAddress.builder()
                .address("Via nuova 26")
                .type(DigitalAddressType.PEC)
                .build());

        addressBook.add(AddressBookEntry.builder()
                .taxId("testIdRecipient")
                .courtesyAddresses(cuortesyAddress)
                .build());
    }

    @Override
    public Optional<AddressBookEntry> getAddresses(String taxId, NotificationSender sender) {
        return addressBook.stream().filter(addressBookEntry -> addressBookEntry.getTaxId().equals(taxId)).findFirst();
    }
}
