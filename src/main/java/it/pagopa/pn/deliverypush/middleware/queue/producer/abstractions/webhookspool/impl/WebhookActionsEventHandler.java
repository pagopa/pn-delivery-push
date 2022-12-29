package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_EVENTFAILED;

@Service
@Slf4j
public class WebhookActionsEventHandler {

    private final WebhookService webhookService;

    public WebhookActionsEventHandler(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public void handleEvent(WebhookAction evt ) {
        log.info( "Received WEBHOOK-ACTION actionType={}", evt.getType());
        try {
            switch (evt.getType()) {
                case REGISTER_EVENT -> doHandleRegisterEvent(evt);
                case PURGE_STREAM_OLDER_THAN, PURGE_STREAM -> doHandlePurgeEvent(evt);
                default ->
                        throw new PnInternalException("Error handling webhook event", ERROR_CODE_WEBHOOK_EVENTFAILED);
            }
        } catch (Exception e) {
            log.error("error handling event", e);
            throw new PnInternalException("Error handling webhook event", ERROR_CODE_WEBHOOK_EVENTFAILED, e);
        }

    }

    private void doHandlePurgeEvent(WebhookAction evt) {
        log.debug("[enter] doHandlePurgeEvent evt={}", evt);
        webhookService
            .purgeEvents(evt.getStreamId(), evt.getEventId(), evt.getType() == WebhookEventType.PURGE_STREAM_OLDER_THAN)
                .block();
        log.debug("[exit] doHandlePurgeEvent evt={}", evt);
    }

    private void doHandleRegisterEvent(WebhookAction evt) {
        log.debug("[enter] doHandleRegisterEvent evt={}", evt);

        webhookService
            .saveEvent(evt.getPaId(), evt.getTimelineId(), evt.getIun())
                .block();
        log.debug("[exit] doHandleRegisterEvent evt={}", evt);
    }

}
