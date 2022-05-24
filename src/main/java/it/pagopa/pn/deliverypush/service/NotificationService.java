package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface NotificationService {
    NotificationInt getNotificationByIun(String iun);
}
