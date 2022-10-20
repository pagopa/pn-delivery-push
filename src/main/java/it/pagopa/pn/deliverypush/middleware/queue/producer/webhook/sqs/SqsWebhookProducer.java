package it.pagopa.pn.deliverypush.middleware.queue.producer.webhook.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsWebhookProducer extends AbstractSqsMomProducer<WebhookEvent> {

    public SqsWebhookProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, WebhookEvent.class );
    }
}
