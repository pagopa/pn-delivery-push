package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import org.springframework.messaging.MessageHeaders;

public interface EventHandler<T> {
    void handle(T payload, MessageHeaders headers);
    SupportedEventType getSupportedEventType();
    Class<T> getPayloadType();
}