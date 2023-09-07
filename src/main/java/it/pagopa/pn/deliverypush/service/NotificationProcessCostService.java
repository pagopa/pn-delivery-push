package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import reactor.core.publisher.Mono;

public interface NotificationProcessCostService {
    Mono<Integer> getPagoPaNotificationBaseCost();
    
    Mono<NotificationProcessCost> notificationProcessCost(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Boolean applyCost);
}
