package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import reactor.core.publisher.Mono;

public interface NotificationProcessCostService {

    int getSendFee();
    Mono<Integer> notificationProcessCostF24(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Integer paFee, Integer vat, String version);
    Mono<NotificationProcessCost> notificationProcessCost(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Boolean applyCost, Integer paFee, Integer vat);
}
