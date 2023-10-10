package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface NotificationProcessCostService {
    Mono<UpdateNotificationCostResponseInt> setNotificationStepCost(int notificationStepCost,
                                                                           String iun,
                                                                           List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients,
                                                                           Instant eventTimestamp,
                                                                           Instant eventStorageTimestamp,
                                                                           UpdateCostPhaseInt updateCostPhase);
    
    Mono<Integer> getPagoPaNotificationBaseCost();
    
    Mono<NotificationProcessCost> notificationProcessCost(String iun, int recIndex, NotificationFeePolicy notificationFeePolicy, Boolean applyCost, Integer paFee);
}
