package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface IoService {
    void sendIOMessage(NotificationInt notification, int recIndex);
}
