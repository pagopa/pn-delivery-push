package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface PaperChannelService {

    void prepareAnalogNotificationForSimpleRegisteredLetter(NotificationInt notification,  Integer recIndex);

    void prepareAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade);

    void sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex, String requestId, PhysicalAddressInt receiverAddress, String productType);

    void sendAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade, String requestId, PhysicalAddressInt receiverAddress, String productType );
}
