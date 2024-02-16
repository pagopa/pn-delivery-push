package it.pagopa.pn.deliverypush.service.impl;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.MessageFormatter;
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
            .switchIfEmpty(Mono.error(new PnWebhookForbiddenException("Pa " + xPagopaPnCxId + " groups (" + join(xPagopaPnCxGroups)+ ") is not allowed to see this streamId " + streamId)));
    }

    protected String join(List<String> list){
        return list == null ? "" : String.join(",", list);
    }

    protected String apiVersion(String xPagopaPnApiVersion){
        return xPagopaPnApiVersion != null ? xPagopaPnApiVersion : pnDeliveryPushConfigs.getWebhook().getCurrentVersion();
    }
    @NotNull
    protected PnAuditLogEvent generateAuditLog(PnAuditLogEventType pnAuditLogEventType, String message, String[] arguments) {
        PnAuditLogEvent pnAuditLogEvent = null;
        String logMessage = MessageFormatter.arrayFormat(message, arguments).getMessage();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent;
        logEvent = auditLogBuilder.before(pnAuditLogEventType, "{}", logMessage)
                .build();
        return logEvent;
    }

    protected String groupString(List<String> groups){
        return groups==null ? null : String.join(",",groups);
    }
}
