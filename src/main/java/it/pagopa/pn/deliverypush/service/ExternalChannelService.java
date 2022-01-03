package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

public interface ExternalChannelService {
    void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient, int sentAttemptMade);

    void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient, String eventId);

    void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient);

    void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation, int sentAttemptMade);
}
