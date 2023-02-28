package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import reactor.core.publisher.Mono;

public interface NotificationCostService {
    Mono<Integer> getPagoPaNotificationBaseCost();

    NotificationCostResponseInt getIunFromPaTaxIdAndNoticeCode(String paTaxId, String noticeCode);

    Mono<NotificationProcessCost> notificationProcessCost(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy);
}
