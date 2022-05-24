package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementCategoryEntity;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Slf4j
public class TimelineElementCategoryEntityConverter  implements AttributeConverter<TimelineElementCategoryEntity> {

    @Override
    public AttributeValue transformFrom(TimelineElementCategoryEntity input) {
        String value = input != null ? input.getValue() : "";
        return EnhancedAttributeValue.fromString(value).toAttributeValue();
    }

    @Override
    public TimelineElementCategoryEntity transformTo(AttributeValue input) {
        if( input.s() != null ) {
            return TimelineElementCategoryEntity.valueOf(input.s());
        }
        return null;
    }

    @Override
    public EnhancedType<TimelineElementCategoryEntity> type() {
        return EnhancedType.of(TimelineElementCategoryEntity.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

}
