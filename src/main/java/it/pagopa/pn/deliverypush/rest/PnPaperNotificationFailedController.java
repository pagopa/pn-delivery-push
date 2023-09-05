package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.PaperNotificationFailedApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponsePaperNotificationFailedDto;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class PnPaperNotificationFailedController implements PaperNotificationFailedApi {

    private final PaperNotificationFailedService service;
    private final TimelineUtils timelineUtils;

    public PnPaperNotificationFailedController(PaperNotificationFailedService service, TimelineUtils timelineUtils) {
        this.service = service;
        this.timelineUtils = timelineUtils;
    }

    @Override
    public Mono<ResponseEntity<Flux<ResponsePaperNotificationFailedDto>>> paperNotificationFailed(
            String recipientInternalId,
            Boolean getAAR,
            final ServerWebExchange exchange
    ) {

        return Mono.fromSupplier(() -> {
            List<ResponsePaperNotificationFailedDto> responses = service.getPaperNotificationByRecipientId(recipientInternalId, getAAR);

            List<ResponsePaperNotificationFailedDto> filtered = responses.stream()
                .filter(notification -> !timelineUtils.checkIsNotificationCancellationRequested(notification.getIun()))
                .toList();

            Flux<ResponsePaperNotificationFailedDto> fluxFacts = Flux.fromIterable(filtered);
            return ResponseEntity.ok(fluxFacts);
        });
    }

}
