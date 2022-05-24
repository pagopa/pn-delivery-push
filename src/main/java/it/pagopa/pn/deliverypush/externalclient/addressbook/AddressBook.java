package it.pagopa.pn.deliverypush.externalclient.addressbook;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;

import java.util.Optional;

public interface AddressBook {
    Optional<AddressBookEntry> getAddresses(String taxId, NotificationSenderInt sender);

}
