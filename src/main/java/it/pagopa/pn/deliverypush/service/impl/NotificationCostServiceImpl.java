package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationCostServiceImpl implements NotificationCostService {

    @Override
    public Integer getNotificationCost(NotificationInt notificationInt, int recIndex) {
        return 100;
    }
}
