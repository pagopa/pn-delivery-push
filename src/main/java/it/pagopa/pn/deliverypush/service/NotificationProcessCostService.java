package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface NotificationProcessCostService {
    Integer getNotificationProfit(NotificationInt notificationInt, int recIndex);
}
