package it.pagopa.pn.deliverypush.middleware.momproducer.emailrequest.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsEmailRequestProducer extends AbstractSqsMomProducer<PnExtChnEmailEvent> implements MomProducer<PnExtChnEmailEvent> {

    public SqsEmailRequestProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, PnExtChnEmailEvent.class);
    }
}
