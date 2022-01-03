package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook2;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AddressBookServiceImpl implements AddressBookService {
    private final AddressBook2 addressBook;

    public AddressBookServiceImpl(AddressBook2 addressBook) {
        this.addressBook = addressBook;
    }

    @Override
    public DigitalAddress retrievePlatformAddress(NotificationRecipient recipient, NotificationSender sender) {

        Optional<AddressBookEntry> addressBookEntryOpt = addressBook.getAddresses(recipient.getTaxId(), sender);

        if (addressBookEntryOpt.isPresent()) {
            DigitalAddresses digitalAddresses = addressBookEntryOpt.get().getDigitalAddresses(); //TODO Valutare se far ritornare un solo indirizzo all'addressbook e non una lista
            DigitalAddress platformAddress = digitalAddresses.getPlatform();
            return platformAddress != null && platformAddress.getAddress() != null ? platformAddress : null;
        }
        return null;
    }


}
