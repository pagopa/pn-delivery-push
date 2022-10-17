package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;

class WebhooksPoolImplTest {

    @Mock
    private MomProducer<WebhookEvent> actionsQueue;

    @Mock
    private Clock clock;

    private WebhooksPoolImpl web;

    @BeforeEach
    public void setup() {
        actionsQueue = Mockito.mock(MomProducer.class);
        clock = Mockito.mock(Clock.class);
        web = new WebhooksPoolImpl(actionsQueue, clock);
    }

    @Test
    void scheduleFutureAction() {

        String uuid = UUID.randomUUID().toString();

        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        Mockito.when(clock.instant()).thenReturn(instant);

        web.scheduleFutureAction(buildWebhookAction());

        Mockito.verify(actionsQueue).push(Mockito.argThat(matches((WebhookEvent tmp) -> tmp.getHeader().getIun().equalsIgnoreCase("004"))));
        
    }

    private WebhookAction buildWebhookAction() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        return WebhookAction.builder()
                .streamId("001")
                .eventId("002")
                .paId("003")
                .iun("004")
                .delay(5)
                .timestamp(instant)
                .timelineId("006")
                .oldStatus("007")
                .newStatus("008")
                .timelineEventCategory("009")
                .type(WebhookEventType.REGISTER_EVENT)
                .build();
    }

    private static <T> ArgumentMatcher<T> matches(Predicate<T> predicate) {
        return new ArgumentMatcher<T>() {

            @SuppressWarnings("unchecked")
            @Override
            public boolean matches(Object argument) {
                return predicate.test((T) argument);
            }
        };
    }
}