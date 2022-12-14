package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.ApiUtil;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationProcessCostResponse;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public Mono<ResponseEntity<NotificationProcessCostResponse>> getNotificationProcessCost(String iun, String pathRecipientIndex, final ServerWebExchange exchange) {
        Mono<Void> result = Mono.empty();
        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                String exampleString = "{ \"amount\" : 200, \"refinementDate\" : \"2000-01-23T04:56:07.000+00:00\" }";
                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
                break;
            }
        }
        return result.then(Mono.empty());
    }
}
