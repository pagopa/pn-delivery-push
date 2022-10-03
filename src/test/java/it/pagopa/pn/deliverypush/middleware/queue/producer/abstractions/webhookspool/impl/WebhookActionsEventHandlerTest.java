package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;

class WebhookActionsEventHandlerTest {

    @Mock
    private WebhookService webhookService;

    private WebhookActionsEventHandler handler;

    @BeforeEach
    public void setup() {
        webhookService = Mockito.mock(WebhookService.class);
        handler = new WebhookActionsEventHandler(webhookService);
    }

    @Test
    void handleEventRegister() {

        WebhookAction action = buildWebhookAction();
        //handler.handleEvent(action);

        // Mockito.verify(webhookService, Mockito.times(1)).saveEvent(action.getPaId(), action.getEventId(), action.getIun(), action.getTimestamp(), action.getOldStatus(), action.getNewStatus(), action.getTimelineEventCategory()).block();
    }

    private WebhookAction buildWebhookAction() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return WebhookAction.builder()
                .type(WebhookEventType.REGISTER_EVENT)
                .paId("001")
                .eventId("002")
                .iun("003")
                .timestamp(instant)
                .oldStatus("old")
                .newStatus("new")
                .timelineEventCategory("time")
                .build();
    }
}