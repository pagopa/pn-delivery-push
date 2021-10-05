package it.pagopa.pn.deliverypush.middleware.momproducer.paperrequest.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsPaperRequestProducer extends AbstractSqsMomProducer<PnExtChnPaperEvent> implements MomProducer<PnExtChnPaperEvent> {

    public SqsPaperRequestProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, PnExtChnPaperEvent.class);
    }
}
