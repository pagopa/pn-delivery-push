package it.pagopa.pn.deliverypush;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.ActionEvent;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookEvent;
import org.springframework.boot.test.mock.mockito.MockBean;

public abstract class MockSQSTest {

    @MockBean(name = "actionsEventProducer")
    private MomProducer<ActionEvent> actionsEventProducer;

    @MockBean(name = "webhookActionsEventProducer")
    private MomProducer<WebhookEvent> webhookActionsEventProducer;

    @MockBean
    private AmazonSQSAsync amazonSQS;

    @MockBean(name = "actionsDoneEventProducer")
    private MomProducer<ActionEvent> actionsDoneEventProducer;
}
