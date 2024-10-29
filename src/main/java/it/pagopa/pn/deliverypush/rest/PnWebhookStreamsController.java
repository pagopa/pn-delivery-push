package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.api.StreamsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.*;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import it.pagopa.pn.deliverypush.utils.MdcKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PnWebhookStreamsController implements StreamsApi {

    private final WebhookStreamsService webhookStreamsService;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV24>> createEventStreamV24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Mono<StreamCreationRequestV24> streamCreationRequest, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] createEventStream xPagopaPnCxId={} xPagopaPnCxGroups={}", xPagopaPnCxId, xPagopaPnCxGroups);

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.createEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamCreationRequest)
                        .map(ResponseEntity::ok));
    }


    @Override
    public Mono<ResponseEntity<Void>> deleteEventStreamV24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] deleteEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
                webhookStreamsService.deleteEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups,xPagopaPnApiVersion, streamId)
                        .then(Mono.just(ResponseEntity.noContent().build()))
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV24>> getEventStreamV24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] getEventStream xPagopaPnCxId={} streamId={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.getEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<Flux<StreamListElement>>> listEventStreamsV24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        log.info("[enter] listEventStreams xPagopaPnCxId={}", xPagopaPnCxId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(webhookStreamsService.listEventStream(xPagopaPnUid, xPagopaPnCxId,xPagopaPnCxGroups, xPagopaPnApiVersion)));
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV24>> updateEventStreamV24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, Mono<StreamRequestV24> streamRequest, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] updateEventStream xPagopaPnCxId={} xPagopaPnCxType={} xPagopaPnCxGroups={} streamId={}", xPagopaPnCxId, xPagopaPnCxType,xPagopaPnCxGroups==null?"":String.join(",", xPagopaPnCxGroups), streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.updateEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId, streamRequest)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV24>> disableEventStreamV24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] disableEventStream xPagopaPnCxId={} uuid={} xPagopaPnCxGroups={}", xPagopaPnCxId, streamId.toString(), xPagopaPnCxGroups==null?"":String.join(",", xPagopaPnCxGroups));

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.disableEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId)
                .map(ResponseEntity::ok)
        );
    }


}
