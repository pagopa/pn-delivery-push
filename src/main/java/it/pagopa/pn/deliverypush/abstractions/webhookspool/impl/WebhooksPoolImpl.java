package it.pagopa.pn.deliverypush.abstractions.webhookspool.impl;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhooksPool;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.service.ActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@Slf4j
public class WebhooksPoolImpl implements WebhooksPool {

    private final MomProducer<WebhookEvent> actionsQueue;
    private final ActionService actionService;
    private final Clock clock;
    private final PnDeliveryPushConfigs configs;

    public WebhooksPoolImpl(MomProducer<WebhookEvent> actionsQueue, ActionService actionService,
                            Clock clock, PnDeliveryPushConfigs configs) {
        this.actionsQueue = actionsQueue;
        this.actionService = actionService;
        this.clock = clock;
        this.configs = configs;
    }

    @Override
    public void addWebhookAction(WebhookAction action, WebhookEventType webhookEventType) {
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
