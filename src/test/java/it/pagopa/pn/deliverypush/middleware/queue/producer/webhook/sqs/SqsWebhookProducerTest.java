package it.pagopa.pn.deliverypush.middleware.queue.producer.webhook.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import software.amazon.awssdk.services.sqs.SqsClient;

class SqsWebhookProducerTest {

    private SqsWebhookProducer sqsProducer;

    @BeforeEach
    void setUp() {
        SqsClient sqsClient = Mockito.any(SqsClient.class);
        String topic = Mockito.anyString();
        ObjectMapper objectMapper = Mockito.any(ObjectMapper.class);
        sqsProducer = new SqsWebhookProducer(sqsClient, topic, objectMapper);
    }

}