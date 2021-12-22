package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;

public interface ExternalChannel {
    void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, String iun, NotificationRecipient recipient);

    void sendNotificationForRegisteredLetter(Notification notification, String address);

    void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, String iun, NotificationRecipient recipient, boolean investigation);

}
