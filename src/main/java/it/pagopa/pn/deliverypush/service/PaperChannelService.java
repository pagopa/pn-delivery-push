package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface PaperChannelService {
    String PREPARE_ANALOG_NOTIFICATION = "PREPARE ANALOG NOTIFICATION";
    String SEND_ANALOG_NOTIFICATION = "SEND ANALOG NOTIFICATION";

    void prepareAnalogNotificationForSimpleRegisteredLetter(NotificationInt notification,  Integer recIndex);

    void prepareAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade);

    String sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex, String requestId, PhysicalAddressInt receiverAddress, String productType);

    String sendAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade, String requestId, PhysicalAddressInt receiverAddress, String productType);
}
