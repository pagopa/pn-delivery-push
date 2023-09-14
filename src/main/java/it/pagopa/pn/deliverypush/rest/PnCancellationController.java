package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.NotificationCancellationApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.StatusDetail;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
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
    
    @Override
    public  Mono<ResponseEntity<RequestStatus>> notificationCancellation(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId, 
            String iun,
            List<String> xPagopaPnCxGroups,
            final ServerWebExchange exchange) {
        return notificationCancellationService.startCancellationProcess(iun, xPagopaPnCxId, xPagopaPnCxType)
                .map(statusdetail -> ResponseEntity.accepted().body(RequestStatus.builder()
                                    .status("OK")
                                    .details(List.of(SmartMapper.mapToClass(statusdetail, StatusDetail.class)))
                                    .build())
                );
    }
}
