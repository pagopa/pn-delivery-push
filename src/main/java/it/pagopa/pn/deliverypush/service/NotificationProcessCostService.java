package it.pagopa.pn.deliverypush.service;

import reactor.core.publisher.Mono;

public interface NotificationProcessCostService {
    Mono<Integer> getNotificationProcessCost(String iun, int recIndex);
}
