package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;

public interface NotificationService {
    Notification getNotificationByIun(String iun);

}
