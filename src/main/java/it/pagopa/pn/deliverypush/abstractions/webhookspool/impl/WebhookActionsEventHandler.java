package it.pagopa.pn.deliverypush.abstractions.webhookspool.impl;


import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebhookActionsEventHandler {

    private final WebhookService webhookService;

    public WebhookActionsEventHandler(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    public void handleEvent(WebhookAction evt ) {
        log.info( "Received WEBHOOK-ACTION actionType={}", evt.getType());
        if (evt.getType() == WebhookEventType.REGISTER_EVENT)
            doHandleRegisterEvent(evt);
        else
            doHandlePurgeEvent(evt);

    }

    private void doHandlePurgeEvent(WebhookAction evt) {
        webhookService
            .purgeEvents(evt.getStreamId(), evt.getEventId(), evt.getType() == WebhookEventType.PURGE_STREAM_OLDER_THAN)
                .subscribe();
    }

    private void doHandleRegisterEvent(WebhookAction evt) {
        webhookService
            .saveEvent(evt.getStreamId(), evt.getEventId(), evt.getIun(), evt.getRequestId(), evt.getTimestamp(), evt.getNewStatus(), evt.getTimelineEventCategory())
                .subscribe();
    }

}
