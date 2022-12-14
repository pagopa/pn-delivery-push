package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;

public class NotificationProcessCostServiceImpl implements NotificationProcessCostService {
    @Override
    public Integer getNotificationProfit(NotificationInt notificationInt, int recIndex) {
        return 100;
    }
}
