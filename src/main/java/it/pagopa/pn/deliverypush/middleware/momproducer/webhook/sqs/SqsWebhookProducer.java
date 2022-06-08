package it.pagopa.pn.deliverypush.middleware.momproducer.webhook.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsWebhookProducer extends AbstractSqsMomProducer<ActionEvent> {

    public SqsWebhookProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, ActionEvent.class );
    }
}
