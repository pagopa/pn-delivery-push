package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;

public interface AddressBookService {
    DigitalAddress retrievePlatformAddress(NotificationRecipient recipient, NotificationSender sender);
}
