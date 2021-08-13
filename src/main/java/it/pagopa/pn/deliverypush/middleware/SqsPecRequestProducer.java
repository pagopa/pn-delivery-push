package it.pagopa.pn.deliverypush.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsPecRequestProducer extends AbstractSqsMomProducer<PnExtChnPecEvent> implements MomProducer<PnExtChnPecEvent> {

    protected SqsPecRequestProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, PnExtChnPecEvent.class);
    }
}
