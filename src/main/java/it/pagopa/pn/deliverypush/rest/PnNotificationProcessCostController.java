package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationProcessCostResponse;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class PnNotificationProcessCostController implements NotificationProcessCostApi {
    private final NotificationProcessCostService notificationProcessCostService;

    public PnNotificationProcessCostController(NotificationProcessCostService notificationProcessCostService) {
        this.notificationProcessCostService = notificationProcessCostService;
    }

    @Override
    public Mono<ResponseEntity<NotificationProcessCostResponse>> getNotificationProcessCost(String iun, Integer pathRecipientIndex, final ServerWebExchange exchange) {
        return notificationProcessCostService.getNotificationProcessCost(iun, pathRecipientIndex)
                .map(response -> ResponseEntity
                        .ok()
                        .body(
                                NotificationProcessCostResponse.builder()
                                        .amount(response)
                                        .build()
                        ));
    }
}
