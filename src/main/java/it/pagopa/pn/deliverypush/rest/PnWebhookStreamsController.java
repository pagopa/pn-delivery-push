package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.api.StreamsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
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

    private final WebhookStreamsService webhookStreamsService;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV23>> createEventStreamV23(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Mono<StreamCreationRequestV23> streamCreationRequestV23, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] createEventStream xPagopaPnCxId={} xPagopaPnCxGroups={}", xPagopaPnCxId, xPagopaPnCxGroups);

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.createEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamCreationRequestV23)
                        .map(ResponseEntity::ok));
    }


    @Override
    public Mono<ResponseEntity<Void>> deleteEventStreamV23(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] deleteEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
                webhookStreamsService.deleteEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups,xPagopaPnApiVersion, streamId)
                        .then(Mono.just(ResponseEntity.noContent().build()))
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV23>> getEventStreamV23(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] getEventStream xPagopaPnCxId={} streamId={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.getEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<Flux<StreamListElement>>> listEventStreamsV23(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        log.info("[enter] listEventStreams xPagopaPnCxId={}", xPagopaPnCxId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(webhookStreamsService.listEventStream(xPagopaPnUid, xPagopaPnCxId,xPagopaPnCxGroups, xPagopaPnApiVersion)));
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV23>> updateEventStreamV23(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, Mono<StreamRequestV23> streamRequestV23, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] updateEventStream xPagopaPnCxId={} xPagopaPnCxType={} xPagopaPnCxGroups={} streamId={}", xPagopaPnCxId, xPagopaPnCxType,xPagopaPnCxGroups==null?"":String.join(",", xPagopaPnCxGroups), streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.updateEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId, streamRequestV23)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV23>> disableEventStreamV23(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);
        log.info("[enter] disableEventStream xPagopaPnCxId={} uuid={} xPagopaPnCxGroups={}", xPagopaPnCxId, streamId.toString(), xPagopaPnCxGroups==null?"":String.join(",", xPagopaPnCxGroups));

        return MDCUtils.addMDCToContextAndExecute(
            webhookStreamsService.disableEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId)
                .map(ResponseEntity::ok)
        );
    }


}
