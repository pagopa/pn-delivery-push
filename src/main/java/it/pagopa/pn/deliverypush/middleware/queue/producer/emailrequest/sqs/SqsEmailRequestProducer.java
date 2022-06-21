package it.pagopa.pn.deliverypush.middleware.queue.producer.emailrequest.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * @deprecated
 * Deprecata in attesa di un mock di externalChannel con le nuove api
 */
@Deprecated(since = "PN-612", forRemoval = true)
public class SqsEmailRequestProducer extends AbstractSqsMomProducer<PnExtChnEmailEvent> implements MomProducer<PnExtChnEmailEvent> {

    public SqsEmailRequestProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, PnExtChnEmailEvent.class);
    }
}
