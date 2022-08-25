package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhooksPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

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
    public void scheduleFutureAction(WebhookAction action) {
        // TODO prevedere la gestione del delay passato nella action in fase di inserimento
        addWebhookAction(action);
    }

    private void addWebhookAction(WebhookAction action ) {
        actionsQueue.push( WebhookEvent.builder()
                .header( StandardEventHeader.builder()
                        .publisher("deliveryPush")
                        .iun( action.getIun() )
                        .eventId(UUID.randomUUID().toString())
                        .createdAt( clock.instant() )
                        .eventType( WebhookActionEventType.WEBHOOK_ACTION_GENERIC.name())
                        .build()
                )
                .payload( action )
                .build()
        );
    }
}
