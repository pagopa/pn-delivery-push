package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Slf4j
public class TimelineElementDetailsEntityConverter implements AttributeConverter<TimelineElementDetailsEntity> {
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(TimelineElementDetailsEntity input) {
        String jsonValue;
        try {
            jsonValue = jsonMapper.writeValueAsString( input );
        } catch (JsonProcessingException ex) {
            log.error("exception in processing json ex {}", ex);
            throw new PnInternalException(ex.getMessage());
        }
        return EnhancedAttributeValue.fromString(jsonValue).toAttributeValue();
    }

    @Override
    public TimelineElementDetailsEntity transformTo(AttributeValue input) {
        if( input.s() != null ) {
            try {
                return jsonMapper.readValue( input.s(), TimelineElementDetailsEntity.class );
            } catch (JsonProcessingException ex) {
                log.error("exception in processing json ex {}", ex);
                throw new PnInternalException(ex.getMessage());
            }
        }

        return null;
    }

    @Override
    public EnhancedType<TimelineElementDetailsEntity> type() {
        return EnhancedType.of(TimelineElementDetailsEntity.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
