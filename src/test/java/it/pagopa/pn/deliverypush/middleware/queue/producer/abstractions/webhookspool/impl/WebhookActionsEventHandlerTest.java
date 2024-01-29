package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.service.WebhookEventsService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class WebhookActionsEventHandlerTest {

    @Mock
    private WebhookEventsService webhookService;

    private WebhookActionsEventHandler handler;

    @BeforeEach
    public void setup() {
        webhookService = Mockito.mock(WebhookEventsService.class);
        handler = new WebhookActionsEventHandler(webhookService);
    }

    @Test
    void handleEventRegister() {
        // GIVEN
        WebhookAction action = buildWebhookAction();
        Mockito.when(webhookService.saveEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());

        // WHEN
        handler.handleEvent(action);

        // THEN
        Mockito.verify(webhookService, Mockito.times(1))
                .saveEvent(action.getPaId(), action.getTimelineId(), action.getIun());
    }


    @Test
    void handleEventPurge() {
        // GIVEN
        WebhookAction action = buildWebhookActionPurge();
        Mockito.when(webhookService.purgeEvents(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(Mono.empty());

        // WHEN
        handler.handleEvent(action);

        // THEN
        Mockito.verify(webhookService, Mockito.times(1))
                .purgeEvents(action.getStreamId(), action.getEventId(), false);
    }

    private WebhookAction buildWebhookAction() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return WebhookAction.builder()
                .type(WebhookEventType.REGISTER_EVENT)
                .paId("001")
                .eventId("002")
                .iun("003")
                .timelineId("timelineId")
                .build();
    }


    private WebhookAction buildWebhookActionPurge() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return WebhookAction.builder()
                .type(WebhookEventType.PURGE_STREAM)
                .paId("001")
                .streamId("001")
                .eventId("002")
                .iun("003")
                .timelineId("timelineId")
                .build();
    }
}