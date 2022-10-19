package it.pagopa.pn.deliverypush.middleware.queue.producer.action.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.ActionEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsActionProducer extends AbstractSqsMomProducer<ActionEvent> {

    public SqsActionProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, ActionEvent.class );
    }
}
