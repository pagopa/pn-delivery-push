package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.deliverypush.external.AddressBook;
import it.pagopa.pn.deliverypush.external.AddressBookEntry;

import java.util.Collection;
import java.util.Optional;

public class AddressBookMock implements AddressBook {
    private final Collection<AddressBookEntry> addressBook;

    public AddressBookMock(Collection<AddressBookEntry> addressBook) {
        this.addressBook = addressBook;
    }

    @Override
    public Optional<AddressBookEntry> getAddresses(String taxId, NotificationSender sender) {
        return addressBook.stream().filter(addressBookEntry -> addressBookEntry.getTaxId().equals(taxId)).findFirst();
    }
}
