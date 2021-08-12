package it.pagopa.pn.deliverypush.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.mom.sqs.GenericSqsMOM;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.events.NewNotificationEvt;
import it.pagopa.pn.deliverypush.events.PecRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


@ConditionalOnProperty( name="pn.mom", havingValue = "sqs")
@Component
public class SqsPecRequestMOM extends GenericSqsMOM<PecRequest> implements PecRequestMOM {

    public SqsPecRequestMOM(SqsAsyncClient sqs, ObjectMapper objMapper, PnDeliveryPushConfigs cfg) {
        super( sqs, objMapper, PecRequest.class, cfg.getTopics().getToExternalChannel() );
    }

}
