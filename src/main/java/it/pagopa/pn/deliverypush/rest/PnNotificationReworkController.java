package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.NotificationReworkApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkItemsResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkResponse;
import it.pagopa.pn.deliverypush.service.NotificationReworkService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationReworkMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
public class PnNotificationReworkController implements NotificationReworkApi {

    private final NotificationReworkService notificationReworkService;

    @Override
    public Mono<ResponseEntity<ReworkResponse>> notificationRework(String iun, Mono<ReworkRequest> reworkRequest, final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, "NOTIFICATION_REWORK_" + iun);
        return reworkRequest.flatMap(request -> notificationReworkService.createNotificationReworkRequest(NotificationReworkMapper.externalToInternal(request, iun)))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<ReworkItemsResponse>> retrieveNotificationRework(String iun, String reworkId, final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, "NOTIFICATION_REWORK_" + iun + "_" + reworkId);
        return notificationReworkService.retrieveNotificationRework(iun, reworkId)
                .map(ResponseEntity::ok);
    }
}
