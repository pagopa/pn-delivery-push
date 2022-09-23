package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementCategoryEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class TimelineElementCategoryEntityConverterTest {

    private TimelineElementCategoryEntityConverter converter;

    @BeforeEach
    public void setup() {
        converter = new TimelineElementCategoryEntityConverter();
    }

    @Test
    void transformFrom() {

        AttributeValue actual = converter.transformFrom(buildTimelineElementCategoryEntity());

        Assertions.assertNotNull(actual);
    }

    @Test
    void transformTo() {

        TimelineElementCategoryEntity actual = converter.transformTo(buildAttribute());

        Assertions.assertNotNull(actual);
    }

    @Test
    void type() {
        EnhancedType<TimelineElementCategoryEntity> actual = converter.type();

        Assertions.assertNotNull(actual);
    }


    @Test
    void attributeValueType() {

        AttributeValueType actual = converter.attributeValueType();

        Assertions.assertNotNull(actual);
    }

    private TimelineElementCategoryEntity buildTimelineElementCategoryEntity() {
        return TimelineElementCategoryEntity.REFINEMENT;
    }

    private AttributeValue buildAttribute() {
        return AttributeValue.builder()
                .s("REQUEST_ACCEPTED")
                .build();
    }
}