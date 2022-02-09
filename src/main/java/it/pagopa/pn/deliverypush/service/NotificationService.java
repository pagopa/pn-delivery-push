package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;

public interface NotificationService {
    Notification getNotificationByIun(String iun);

    NotificationRecipient getRecipientFromNotification(Notification notification, String taxId);

    void updateNotification(Notification notification);
}
