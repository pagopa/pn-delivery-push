package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ReceivedLegalFactGenerationHandlerTest {

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private ReceivedLegalFactGenerationHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.SCHEDULE_RECEIVED_LEGALFACT_GENERATION, handler.getSupportedEventType());
    }

    @Test
    void handleSaveNotificationReceivedLegalFactsWhenNotificationNotCancelled() {
        Action action = Action.builder()
                .iun("iun_123")
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);

        handler.handle(action, headers);

        Mockito.verify(receivedLegalFactCreationRequest).saveNotificationReceivedLegalFacts("iun_123");
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleDoesNotExecuteSaveNotificationReceivedLegalFactsWhenNotificationIsCancelled() {
        Action action = Action.builder().iun("iun_123").build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(true);

        handler.handle(action, headers);

        Mockito.verify(receivedLegalFactCreationRequest, Mockito.never()).saveNotificationReceivedLegalFacts("iun_123");
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_123")
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);
        Mockito.doThrow(new RuntimeException("Validation error")).when(receivedLegalFactCreationRequest).saveNotificationReceivedLegalFacts("iun_123");

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(receivedLegalFactCreationRequest).saveNotificationReceivedLegalFacts("iun_123");
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

}