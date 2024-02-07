package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class TimelineElementJsonConverter {
    private final ObjectMapper objectMapper;


    public String entityToJson(TimelineElementEntity entity) {
        Map<String, Object> objectHashMap = objectMapper.convertValue(entity, HashMap.class);
        try {
            return objectMapper.writeValueAsString(objectHashMap);
        } catch (JsonProcessingException ex) {
            log.error("Timeline element entity not converted into JSON");
            return null;
        }
    }


}
