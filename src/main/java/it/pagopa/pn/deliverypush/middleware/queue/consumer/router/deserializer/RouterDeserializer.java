package it.pagopa.pn.deliverypush.middleware.queue.consumer.router.deserializer;

import org.springframework.messaging.Message;

public interface RouterDeserializer {
    /**
     * Deserializes the given payload into an object of type T.
     *
     * @param payload the payload to deserialize
     * @param <T> the type of the object to return
     * @return an object of type T
     */
    <T> T deserialize(Message<?> payload, Class<T> targetType);
}
