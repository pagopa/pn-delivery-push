package it.pagopa.pn.deliverypush.abstractions.webhookspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhooksPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@Slf4j
public class WebhooksPoolImpl implements WebhooksPool {

    private final MomProducer<WebhookEvent> actionsQueue;

    private final Clock clock;


    public WebhooksPoolImpl(MomProducer<WebhookEvent> actionsQueue,
                            Clock clock ) {
        this.actionsQueue = actionsQueue;
        this.clock = clock;
    }


    @Override
    public void scheduleFutureAction(WebhookAction action, WebhookEventType webhookEventType) {
        if ( Instant.now().isAfter( action.getNotBefore() )) {
            action = action.toBuilder()
                    .notBefore( Instant.now().plusSeconds(1))
                    .build();
        }
        addWebhookAction(action, webhookEventType);
    }

    private void addWebhookAction(WebhookAction action, WebhookEventType webhookEventType) {
        actionsQueue.push( WebhookEvent.builder()
                .header( StandardEventHeader.builder()
                        .publisher("deliveryPush")
                        .iun( action.getIun() )
                        .eventId( action.getEventId() )
                        .createdAt( clock.instant() )
                        .eventType( webhookEventType.name() )
                        .build()
                )
                .payload( action )
                .build()
        );
    }
}
