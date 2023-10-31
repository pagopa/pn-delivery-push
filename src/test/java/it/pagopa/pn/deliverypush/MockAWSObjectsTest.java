package it.pagopa.pn.deliverypush;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.PnEventInboundService;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.ActionEvent;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookEvent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
public abstract class MockAWSObjectsTest extends MockActionPoolTest {

    @MockBean(name = "actionsEventProducer")
    private MomProducer<ActionEvent> actionsEventProducer;

    @MockBean(name = "webhookActionsEventProducer")
    private MomProducer<WebhookEvent> webhookActionsEventProducer;

    @MockBean
    private AmazonSQSAsync amazonSQS;

    @MockBean(name = "actionsDoneEventProducer")
    private MomProducer<ActionEvent> actionsDoneEventProducer;

    @MockBean
    private PnEventInboundService pnEventInboundService;

    @MockBean
    private DynamoDbClient dynamoDbClient;
}
