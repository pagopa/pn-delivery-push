package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
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
class DocumentCreationResponseEventHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private DocumentCreationResponseHandler documentCreationResponseHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private DocumentCreationResponseEventHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.DOCUMENT_CREATION_RESPONSE, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesSkipsVerifyCancellation() {
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("file_key")
                .documentCreationType(DocumentCreationTypeInt.NOTIFICATION_CANCELLED)
                .build();
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .details(details)
                .build();

        handler.handle(action, headers);

        Mockito.verify(documentCreationResponseHandler).handleResponseReceived("iun_123", 0, details);
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleExecutesWhenNotificationNotCancelled() {
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("file_key")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .build();
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .details(details)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);

        handler.handle(action, headers);

        Mockito.verify(documentCreationResponseHandler).handleResponseReceived("iun_123", 0, details);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleDoesNotExecuteWhenNotificationCancelled() {
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("file_key")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .build();
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .details(details)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(true);

        handler.handle(action, headers);

        Mockito.verify(documentCreationResponseHandler, Mockito.never()).handleResponseReceived("iun_123", 0, details);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .key("file_key")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .build();
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .details(details)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);
        Mockito.doThrow(new RuntimeException("Validation error")).when(documentCreationResponseHandler).handleResponseReceived("iun_123", 0, details);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(documentCreationResponseHandler).handleResponseReceived("iun_123", 0, details);

        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }
}