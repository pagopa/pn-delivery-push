package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationProcessCostResponse;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class PnNotificationProcessCostController implements NotificationProcessCostApi {

    private final NotificationProcessCostService service;
    
    @Override
    public  Mono<ResponseEntity<NotificationProcessCostResponse>> notificationProcessCost(String iun, 
                                                                                          Integer recIndex, 
                                                                                          NotificationFeePolicy notificationFeePolicy, 
                                                                                          final ServerWebExchange exchange) {
        return service.notificationProcessCost(iun, recIndex, notificationFeePolicy)
                .map(response -> ResponseEntity.ok().body(mapResponse(response)));
    }

    private NotificationProcessCostResponse mapResponse(NotificationProcessCost response) {
        return NotificationProcessCostResponse.builder()
        .amount(response.getCost())
        .refinementDate(response.getRefinementDate())
        .notificationViewDate(response.getNotificationViewDate())
        .build();
    }

}
