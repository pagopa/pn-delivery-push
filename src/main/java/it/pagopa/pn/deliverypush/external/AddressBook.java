package it.pagopa.pn.deliverypush.external;

import it.pagopa.pn.api.dto.notification.NotificationSender;

import java.util.Optional;

public interface AddressBook {
    Optional<AddressBookEntry> getAddresses(String taxId, NotificationSender sender);

}
