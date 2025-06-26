package it.pagopa.pn.deliverypush.middleware.queue.consumer.router;

import it.pagopa.pn.deliverypush.exceptions.PnEventRouterException;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_INVALID_SUPPORTED_EVENT;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_MULTIPLE_HANDLERS_FOUND;

@Component
@Slf4j
public class EventHandlerRegistry {
    private final List<EventHandler<?>> eventHandlers;
    private final Map<String, EventHandler<?>> handlerRegistry = new HashMap<>();

    /**
     * Costruttore che accetta una lista di EventHandler. Spring è in grado di iniettare automaticamente
     * tutti i bean di tipo EventHandler registrati nel contesto dell'applicazione.
     * E sfruttando questa lista, vengono registrati gli handler al momento della creazione del bean.
     * @param eventHandlers la lista di EventHandler da registrare
     */
    public EventHandlerRegistry(List<EventHandler<?>> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @PostConstruct
    public void initialize() {
        if (eventHandlers.isEmpty()) {
            log.warn("No event handlers found. Please ensure that event handlers are properly configured.");
            return;
        }

        for (EventHandler<?> handler : eventHandlers) {
            registerHandler(handler);
        }

        log.info("Initialized event handler registry with {} handlers", handlerRegistry.size());
    }

    private void registerHandler(EventHandler<?> handler) {
        SupportedEventType eventType = handler.getSupportedEventType();

        if(Objects.isNull(handler.getSupportedEventType())) {
            String errorMessage = "Event handler " + handler.getClass().getName() + " does not have a valid supported event type.";
            throw new PnEventRouterException(errorMessage, ERROR_CODE_DELIVERYPUSH_ROUTER_INVALID_SUPPORTED_EVENT);
        }

        if (handlerRegistry.containsKey(handler.getSupportedEventType().name())) {
            String errorMessage = "Multiple handlers found for event type: " + handler.getSupportedEventType();
            throw new PnEventRouterException(errorMessage, ERROR_CODE_DELIVERYPUSH_ROUTER_MULTIPLE_HANDLERS_FOUND);
        }

        handlerRegistry.put(eventType.name(), handler);
        log.info("Registered handler {} for event type: {}", handler.getClass().getName(), eventType);
    }

    /**
     * Recupera un handler per il tipo di evento specificato
     */
    public <T> Optional<EventHandler<T>> getHandler(String eventType) {
        @SuppressWarnings("unchecked")
        EventHandler<T> handler = (EventHandler<T>) handlerRegistry.get(eventType);
        return Optional.ofNullable(handler);
    }

}
