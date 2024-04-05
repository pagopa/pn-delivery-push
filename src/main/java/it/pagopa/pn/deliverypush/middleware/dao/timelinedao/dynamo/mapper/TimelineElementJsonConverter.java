package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TimelineElementJsonConverter {
    private static final String LOG_MSG = "Timeline element entity not converted into JSON";
    private ObjectMapper objectMapper;

    public TimelineElementJsonConverter(ObjectMapper objectMapper){
        this.objectMapper = objectMapper.copy();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String entityToJson(TimelineElementEntity entity) {
        Map<String, Object> objectHashMap = objectMapper.convertValue(entity, HashMap.class);
        try {
            return objectMapper.writeValueAsString(objectHashMap);
        } catch (JsonProcessingException ex) {
            log.error(LOG_MSG, ex);
            throw new PnInternalException(LOG_MSG, ERROR_CODE_PN_GENERIC_ERROR);
        }
    }

    public TimelineElementEntity jsonToEntity(String json) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            return objectMapper.readValue(json, TimelineElementEntity.class);
        } catch (JsonProcessingException e) {
            log.error(LOG_MSG,e);
            throw new PnInternalException(LOG_MSG, ERROR_CODE_PN_GENERIC_ERROR);
        }
    }
}