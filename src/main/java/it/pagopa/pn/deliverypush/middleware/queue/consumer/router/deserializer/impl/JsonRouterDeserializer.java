package it.pagopa.pn.deliverypush.middleware.queue.consumer.router.deserializer.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.exceptions.PnEventRouterException;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.deserializer.RouterDeserializer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_DESERIALIZATION;

@Component("jsonRouterDeserializer")
@AllArgsConstructor
@Slf4j
public class JsonRouterDeserializer implements RouterDeserializer {
    private final ObjectMapper objectMapper;

    @Override
    public <T> T deserialize(Message<?> message, Class<T> targetType) {
        log.info("Deserializing message with payload: {}", message.getPayload());

        if (!(message.getPayload() instanceof String)) {
            throw new PnEventRouterException("Message payload must be a string", ERROR_CODE_DELIVERYPUSH_ROUTER_DESERIALIZATION);
        }

        try {
            return objectMapper.readValue(message.getPayload().toString(), targetType);
        } catch (JsonProcessingException e) {
            throw new PnEventRouterException("Error deserializing message payload", ERROR_CODE_DELIVERYPUSH_ROUTER_DESERIALIZATION, e);
        }
    }
}
