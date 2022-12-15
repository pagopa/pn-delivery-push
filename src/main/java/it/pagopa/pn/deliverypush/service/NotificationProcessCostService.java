package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface NotificationProcessCostService {
    Integer getNotificationProcessCost(NotificationInt notificationInt, int recIndex);
}
