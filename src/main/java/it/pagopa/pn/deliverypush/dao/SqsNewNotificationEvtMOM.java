package it.pagopa.pn.deliverypush.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.mom.sqs.GenericSqsMOM;
import it.pagopa.pn.deliverypush.events.NewNotificationEvt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


@ConditionalOnProperty( name="pn.mom", havingValue = "sqs")
@Component
public class SqsNewNotificationEvtMOM extends GenericSqsMOM<NewNotificationEvt> implements NewNotificationEvtMOM {

    public SqsNewNotificationEvtMOM(SqsAsyncClient sqs, ObjectMapper objMapper) {
        super( sqs, objMapper, NewNotificationEvt.class, "new_notification_evt" );
    }

}
