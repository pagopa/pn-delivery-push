package it.pagopa.pn.deliverypush.middleware.queue.consumer.router;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.exceptions.PnEventRouterException;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.deserializer.RouterDeserializer;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.function.Function;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_EVENT_TYPE_MISSING;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_HANDLER_NOT_FOUND;

@Component
@Slf4j
public class EventRouter {

    private final EventHandlerRegistry handlerRegistry;
    private final RouterDeserializer defaultDeserializer;

    @Autowired
    public EventRouter(EventHandlerRegistry handlerRegistry, @Qualifier("jsonRouterDeserializer") RouterDeserializer deserializer) {
        this.handlerRegistry = handlerRegistry;
        this.defaultDeserializer = deserializer;
    }

    /**
     * Route the message to the appropriate handler based on the default routing configuration.
     *
     * @param message The message to route.
     * @param <T>     The type of the payload in the message.
     */
    public <T> void route(Message<T> message) {
        route(message, RoutingConfig.builder().build());
    }

    /**
     * Route the message to the appropriate handler based on the provided routing configuration.
     *
     * @param message The message to route.
     * @param config  The routing configuration. If not provided, default routing behavior will be used.
     * @param <T>     The type of the payload in the message.
     */
    @SuppressWarnings("unchecked")
    public <T> void route(Message<T> message, RoutingConfig config) {
        setMdc(message);
        String eventType = extractEventType(message, config);

        if (!StringUtils.hasText(eventType)) {
            throw new PnEventRouterException("Event type must be provided", ERROR_CODE_DELIVERYPUSH_ROUTER_EVENT_TYPE_MISSING);
        }

        log.info("Routing message with event type: {}", eventType);

        String queueName = config.getQueueNameExtractor().apply(message);

        EventHandler<T> handler = (EventHandler<T>) handlerRegistry.getHandler(eventType)
                .orElseThrow(() -> new PnEventRouterException("No handler found for event type: " + eventType + " in queue: " + queueName, ERROR_CODE_DELIVERYPUSH_ROUTER_HANDLER_NOT_FOUND));

        T payload = extractPayload(message, handler, config);
        handler.handle(payload, message.getHeaders());
    }

    private <T> String extractEventType(Message<T> message, RoutingConfig config) {
        if( config.getEventType() != null) {
            return config.getEventType();
        }

        return config.getEventTypeExtractor().apply(message);
    }

    private void setMdc(Message<?> message) {
        MessageHeaders messageHeaders = message.getHeaders();
        MDCUtils.clearMDCKeys();

        if (messageHeaders.containsKey("aws_messageId")){
            String awsMessageId = messageHeaders.get("aws_messageId", String.class);
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }

        if (messageHeaders.containsKey("X-Amzn-Trace-Id")){
            String traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }

        String iun = (String) message.getHeaders().get("iun");
        if(iun != null){
            MDC.put(MDCUtils.MDC_PN_IUN_KEY, iun);
        }
    }

    private <T> T extractPayload(Message<T> message, EventHandler<T> handler, RoutingConfig config) {
        if (config.getCustomDeserializer() != null) {
            return config.getCustomDeserializer().deserialize(message, handler.getPayloadType());
        } else if (config.isDeserializePayload()) {
            return defaultDeserializer.deserialize(message, handler.getPayloadType());
        } else {
            return message.getPayload();
        }
    }

    /**
     * Oggetto di configurazione per definire il comportamento del routing degli eventi.
     * In caso non venga fornita una configurazione, verranno utilizzati i valori predefiniti.
     */
    @Data
    @Builder
    public static class RoutingConfig {
        /**
         * Funzione per estrarre il nome della coda dal messaggio.
         * <br>
         * Default: estrae il valore dalla chiave "aws_receivedQueue" negli headers del messaggio.
         */
        @Builder.Default
        private Function<Message<?>, String> queueNameExtractor =
                message -> (String) message.getHeaders().get("aws_receivedQueue");

        /**
         * Funzione per estrarre il tipo di evento dal messaggio.
         * <br>
         * Default: estrae il valore dalla chiave "eventType" negli headers del messaggio. Ma se è stato specificato un valore
         * per il campo {@link RoutingConfig#eventType}, questo verrà utilizzato come tipo di evento.
         */
        @Builder.Default
        private Function<Message<?>, String> eventTypeExtractor =
                message -> (String) message.getHeaders().get("eventType");

        private String eventType;

        /**
         * Indica se il payload del messaggio deve essere deserializzato prima di essere passato all'handler.
         * <br>
         * Default: false, il payload viene passato direttamente all'handler senza deserializzazione.
         * <br>
         * Se impostato a true, il payload verrà deserializzato utilizzando il deserializer predefinito o quello personalizzato.
         * (Se fornito il {@link RoutingConfig#customDeserializer}, questo avrà la precedenza)
         */
        @Builder.Default
        private boolean deserializePayload = false;

        /**
         * Deserializer personalizzato da utilizzare per deserializzare il payload del messaggio.
         */
        private RouterDeserializer customDeserializer;
    }

}