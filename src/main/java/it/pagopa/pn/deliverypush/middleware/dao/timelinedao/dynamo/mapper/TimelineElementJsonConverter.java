package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

@Slf4j
@Component
@AllArgsConstructor
public class TimelineElementJsonConverter {
    private static final String LOG_MSG = "Timeline element entity not converted into JSON";
    private final ObjectMapper objectMapper;


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
