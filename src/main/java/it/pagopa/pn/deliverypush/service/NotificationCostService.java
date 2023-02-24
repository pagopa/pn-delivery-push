package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import reactor.core.publisher.Mono;

public interface NotificationCostService {
    Mono<Integer> getPagoPaNotificationBaseCost(NotificationInt notificationInt, int recIndex);

    NotificationCostResponseInt getIunFromPaTaxIdAndNoticeCode(String paTaxId, String noticeCode);
}
