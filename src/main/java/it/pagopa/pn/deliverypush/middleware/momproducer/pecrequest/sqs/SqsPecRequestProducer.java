package it.pagopa.pn.deliverypush.middleware.momproducer.pecrequest.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * @deprecated
 * Deprecata in attesa di un mock di externalChannel con le nuove api
 */
@Deprecated(since = "PN-612", forRemoval = true)
public class SqsPecRequestProducer extends AbstractSqsMomProducer<PnExtChnPecEvent> implements MomProducer<PnExtChnPecEvent> {

    public SqsPecRequestProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper ) {
        super(sqsClient, topic, objectMapper, PnExtChnPecEvent.class);
    }
}
