package it.pagopa.pn.deliverypush.middleware.queue.producer.action.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.amazon.awssdk.services.sqs.SqsClient;

class SqsActionProducerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private ObjectMapper objectMapper;
    private SqsActionProducer producer;

    @BeforeEach
    void setUp() {
        sqsClient = Mockito.mock(SqsClient.class);
        objectMapper = Mockito.mock(ObjectMapper.class);

        producer = new SqsActionProducer(sqsClient, "Test", objectMapper);
    }
    
}