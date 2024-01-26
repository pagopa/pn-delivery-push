package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.api.StreamsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestv23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponsev23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamUpdateRequestv23;
import it.pagopa.pn.deliverypush.service.WebhookEventsService;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import it.pagopa.pn.deliverypush.utils.MdcKey;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PnWebhookStreamsController implements StreamsApi {

    private final static String API_VERSION = "v23";//IVAN: chiedere a fabrizio se deve arrivare in request
    private final WebhookEventsService webhookEventsService;
    private final WebhookStreamsService webhookStreamsService;


    @Override
    public Mono<ResponseEntity<StreamMetadataResponsev23>> createEventStream(String xPagopaPnUid,
        CxTypeAuthFleet xPagopaPnCxType,
        String xPagopaPnCxId,
        Mono<StreamCreationRequestv23> streamCreationRequestv23,
        List<String> xPagopaPnCxGroups,
        ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] createEventStream xPagopaPnCxId={} xPagopaPnCxGroups={}", xPagopaPnCxId, xPagopaPnCxGroups);

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.createEventStream(xPagopaPnCxId, xPagopaPnCxGroups, API_VERSION, streamCreationRequestv23)
                        .map(ResponseEntity::ok));
    }


    @Override
    public Mono<ResponseEntity<Void>> deleteEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] deleteEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
                webhookStreamsService.deleteEventStream(xPagopaPnCxId, xPagopaPnCxGroups, API_VERSION, streamId)
                        .then(Mono.just(ResponseEntity.noContent().build()))
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponsev23>> getEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId,List<String> xPagopaPnCxGroups, ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] getEventStream xPagopaPnCxId={} streamId={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.getEventStream(xPagopaPnCxId, xPagopaPnCxGroups, API_VERSION, streamId)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<Flux<StreamListElement>>> listEventStreams(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups,  final ServerWebExchange exchange) {
        log.info("[enter] listEventStreams xPagopaPnCxId={}", xPagopaPnCxId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(webhookStreamsService.listEventStream(xPagopaPnCxId,xPagopaPnCxGroups, API_VERSION)));
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponsev23>> updateEventStream(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, Mono<StreamUpdateRequestv23> streamUpdateRequestv23, List<String> xPagopaPnCxGroups,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] updateEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.updateEventStream(xPagopaPnCxId, xPagopaPnCxGroups,API_VERSION, streamId, streamUpdateRequestv23)
                        .map(ResponseEntity::ok)
        );
    }
}
