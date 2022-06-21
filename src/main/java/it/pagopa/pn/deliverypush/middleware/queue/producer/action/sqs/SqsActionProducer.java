package it.pagopa.pn.deliverypush.middleware.queue.producer.action.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionEvent;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsActionProducer extends AbstractSqsMomProducer<ActionEvent> {

    public SqsActionProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, ActionEvent.class );
    }
}
