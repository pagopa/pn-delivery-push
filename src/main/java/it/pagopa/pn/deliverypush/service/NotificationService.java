package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;

public interface NotificationService {
    Notification getNotificationByIun(String iun);

}
