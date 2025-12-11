package it.pagopa.pn.deliverypush;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@EnableAutoConfiguration()
public abstract class MockAWSObjectsTest {
    @MockitoBean
    private DynamoDbClient dynamoDbClient;
}
