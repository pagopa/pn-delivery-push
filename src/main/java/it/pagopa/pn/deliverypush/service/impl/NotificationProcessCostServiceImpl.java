package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;

public class NotificationProcessCostServiceImpl implements NotificationProcessCostService {
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public NotificationProcessCostServiceImpl(PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    @Override
    public Integer getNotificationProcessCost(NotificationInt notificationInt, int recIndex) {
        return pnDeliveryPushConfigs.getNotificationBaseCostForPn();
    }
}
