package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.deliverypush.external.AddressBook;
import it.pagopa.pn.deliverypush.external.AddressBookEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class AddressBookMock implements AddressBook {
    private Collection<AddressBookEntry> addressBook;

    public void clear() {
        this.addressBook = new ArrayList<>();
    }

    public void add(AddressBookEntry addressBookEntry) {
        this.addressBook.add(addressBookEntry);
    }
    
    @Override
    public Optional<AddressBookEntry> getAddresses(String taxId, NotificationSender sender) {
        return addressBook.stream().filter(addressBookEntry -> addressBookEntry.getTaxId().equals(taxId)).findFirst();
    }
}
