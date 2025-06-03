package it.pagopa.pn.deliverypush.middleware.queue.consumer.router;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.pagopa.pn.deliverypush.exceptions.PnEventRouterException;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_INVALID_SUPPORTED_EVENT;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_MULTIPLE_HANDLERS_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventHandlerRegistryTest {
    @Test
    void initializeLogsWarningWhenNoHandlersProvided() {
        // get Logback Logger
        Logger fooLogger = LoggerFactory.getLogger(EventHandlerRegistry.class);

        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        // addAppender is outdated now
        ((ch.qos.logback.classic.Logger)fooLogger).addAppender(listAppender);

        EventHandlerRegistry registry = new EventHandlerRegistry(Collections.emptyList());
        registry.initialize();

        // Verify log warning (use a logging framework test utility if available)
        // JUnit assertions
        List<ILoggingEvent> logsList = listAppender.list;
        Assertions.assertEquals("No event handlers found. Please ensure that event handlers are properly configured.", logsList.get(0)
                .getFormattedMessage());
        Assertions.assertEquals(Level.WARN, logsList.get(0)
                .getLevel());
    }

    @Test
    void initializeRegistersAllHandlersCorrectly() {
        EventHandler<?> handler1 = mock(EventHandler.class);
        EventHandler<?> handler2 = mock(EventHandler.class);
        when(handler1.getSupportedEventType()).thenReturn(SupportedEventType.NEW_NOTIFICATION);
        when(handler2.getSupportedEventType()).thenReturn(SupportedEventType.START_RECIPIENT_WORKFLOW);

        EventHandlerRegistry registry = new EventHandlerRegistry(List.of(handler1, handler2));
        registry.initialize();

        assertTrue(registry.getHandler("NEW_NOTIFICATION").isPresent());
        assertTrue(registry.getHandler("START_RECIPIENT_WORKFLOW").isPresent());
    }

    @Test
    void registerHandlerThrowsExceptionForNullEventType() {
        EventHandler<?> handler = mock(EventHandler.class);
        when(handler.getSupportedEventType()).thenReturn(null);

        EventHandlerRegistry registry = new EventHandlerRegistry(List.of(handler));

        PnEventRouterException exception = assertThrows(PnEventRouterException.class, registry::initialize);

        inspectErrorCode(ERROR_CODE_DELIVERYPUSH_ROUTER_INVALID_SUPPORTED_EVENT, exception);
    }



    @Test
    void registerHandlerThrowsExceptionForDuplicateEventType() {
        EventHandler<?> handler1 = mock(EventHandler.class);
        EventHandler<?> handler2 = mock(EventHandler.class);
        when(handler1.getSupportedEventType()).thenReturn(SupportedEventType.NEW_NOTIFICATION);
        when(handler2.getSupportedEventType()).thenReturn(SupportedEventType.NEW_NOTIFICATION);

        EventHandlerRegistry registry = new EventHandlerRegistry(List.of(handler1, handler2));

        PnEventRouterException exception = assertThrows(PnEventRouterException.class, registry::initialize);
        inspectErrorCode(ERROR_CODE_DELIVERYPUSH_ROUTER_MULTIPLE_HANDLERS_FOUND, exception);
    }

    @Test
    void getHandlerReturnsEmptyOptionalForUnknownEventType() {
        EventHandler<?> handler = mock(EventHandler.class);
        when(handler.getSupportedEventType()).thenReturn(SupportedEventType.NEW_NOTIFICATION);

        EventHandlerRegistry registry = new EventHandlerRegistry(List.of(handler));
        registry.initialize();

        assertTrue(registry.getHandler("UNKNOWN_EVENT").isEmpty());
    }

    private void inspectErrorCode(String expectedErrorCode, PnEventRouterException exception) {
        assertEquals(expectedErrorCode, exception.getProblem().getErrors().get(0).getCode());
    }
}