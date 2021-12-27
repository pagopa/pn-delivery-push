package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

public interface ExternalChannelService {
    void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, NotificationRecipient recipient);

    void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient);

    void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient);

    void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation);
}
