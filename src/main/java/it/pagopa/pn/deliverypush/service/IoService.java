package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface IoService {
    boolean sendIOMessage(NotificationInt notification, int recIndex);
}
