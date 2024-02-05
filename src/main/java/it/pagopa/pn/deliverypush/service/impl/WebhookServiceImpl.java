package it.pagopa.pn.deliverypush.service.impl;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnWebhookForbiddenException;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.service.utils.WebhookUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public abstract class WebhookServiceImpl {

    protected final StreamEntityDao streamEntityDao;
    protected final PnDeliveryPushConfigs pnDeliveryPushConfigs;


    protected enum StreamEntityAccessMode {READ, WRITE};

    protected Mono<StreamEntity> getStreamEntityToRead(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId) {
        return filterEntity(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, streamId, StreamEntityAccessMode.READ);
    }
    protected Mono<StreamEntity> getStreamEntityToWrite(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId) {
        return filterEntity(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, streamId, StreamEntityAccessMode.WRITE);
    }
    private Mono<StreamEntity> filterEntity(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId, StreamEntityAccessMode mode) {
        final String apiV10 = pnDeliveryPushConfigs.getWebhook().getFirstVersion();
        return streamEntityDao.get(xPagopaPnCxId,streamId.toString())
            .switchIfEmpty(Mono.error(
                new PnNotFoundException("Not found"
                    , String.format("Stream %s non found for Pa %s",streamId.toString(),xPagopaPnCxId)
                    , ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND)))
            .filter(streamEntity ->
                apiV10.equals(xPagopaPnApiVersion)
                || (
                    mode == StreamEntityAccessMode.WRITE ?  WebhookUtils.checkGroups(streamEntity.getGroups(), xPagopaPnCxGroups) : WebhookUtils.checkGroups(xPagopaPnCxGroups, streamEntity.getGroups())
                )
            ).filter(streamEntity -> xPagopaPnApiVersion.equals(streamEntity.getVersion())
                || (streamEntity.getVersion() == null && apiV10.equals(xPagopaPnApiVersion))
            )
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " groups (" + String.join(",",xPagopaPnCxGroups)+ ") is not allowed to see this streamId " + streamId)));
    }

}
