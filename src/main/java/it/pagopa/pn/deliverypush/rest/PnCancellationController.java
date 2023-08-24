package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.NotificationCancellationApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestStatus;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@AllArgsConstructor
@CustomLog
public class PnCancellationController implements NotificationCancellationApi {

    private final NotificationCancellationService notificationCancellationService;
    
    public  Mono<ResponseEntity<RequestStatus>> notificationCancellation(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId, 
            String iun,
            List<String> xPagopaPnCxGroups,
            final ServerWebExchange exchange) {
        return notificationCancellationService.startCancellationProcess(iun, xPagopaPnCxId, xPagopaPnCxType)
                .then(
                        Mono.fromCallable(() -> {
                            RequestStatus response = RequestStatus.builder()
                                    .status("OK")
                                    .build();
                            return ResponseEntity.ok(response);
                        })
                );

    }
}
