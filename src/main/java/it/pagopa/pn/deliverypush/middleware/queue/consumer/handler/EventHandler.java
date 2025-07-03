package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import org.springframework.messaging.MessageHeaders;

/**
 * Interface to implement to declare a Bean which can handle events with a specific payload type.
 * @param <T> the type of the payload that this handler processes
 */
public interface EventHandler<T> {
    void handle(T payload, MessageHeaders headers);
    SupportedEventType getSupportedEventType();
    Class<T> getPayloadType();
}