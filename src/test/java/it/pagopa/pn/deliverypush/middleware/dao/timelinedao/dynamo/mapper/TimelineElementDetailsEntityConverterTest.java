package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class TimelineElementDetailsEntityConverterTest {


    private ObjectMapper jsonMapper;

    private TimelineElementDetailsEntityConverter converter;

    @BeforeEach
    void setUp() {
        jsonMapper = new ObjectMapper();
        converter = new TimelineElementDetailsEntityConverter();
    }

    @Test
    void transformFrom() throws JsonProcessingException {
        TimelineElementDetailsEntity input = buildTimelineElementDetailsEntity();

        AttributeValue value = converter.transformFrom(input);

        Assertions.assertNotNull(value);
    }

    @Test
    void transformTo() {
        TimelineElementDetailsEntity expected = buildTimelineElementDetailsEntity();
        AttributeValue value = buildAttribute();

        TimelineElementDetailsEntity actual = converter.transformTo(value);

        Assertions.assertEquals(expected, actual);
    }


    private TimelineElementDetailsEntity buildTimelineElementDetailsEntity() {
        return TimelineElementDetailsEntity.builder()
                .recIndex(0)
                .notificationCost(100)
                .build();
    }

    private AttributeValue buildAttribute() {
        return AttributeValue.builder()
                .s("{\"recIndex\":0,\"notificationCost\":100}")
                .build();
    }
}