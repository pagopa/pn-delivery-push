package it.pagopa.pn.deliverypush.action.it.mockbean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.util.StringUtils;

import java.util.Optional;

public class AbstractCachedSsmParameterConsumerMock implements ParameterConsumer {
    private static final String RADD_EXPERIMENTATION_NAME = "radd-experimentation";
    private static final String jsonRADDExperimentationParameterConsumer = "[\"80078\", \"80124\"]";

    @Override
    public <T> Optional<T> getParameterValue(String storeName, Class<T> aClass) {
        Optional<T> result = Optional.empty();

        if(storeName.startsWith(RADD_EXPERIMENTATION_NAME) &&
                StringUtils.hasText(jsonRADDExperimentationParameterConsumer)){
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                result = Optional.of(objectMapper.readValue(jsonRADDExperimentationParameterConsumer, aClass));
            } catch (JsonProcessingException var7) {
                throw new PnInternalException("[TEST] Unable to deserialize object", "PN_GENERIC_ERROR", var7);
            }
        }
        return result;
    }
}
