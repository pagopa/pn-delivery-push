package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
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
class CustomNotificationViewedEventHandlerTest {
    @Mock
    private NotificationViewedRequestHandler notificationViewedRequestHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private CustomNotificationViewedEventHandler handler;

    private static final PnDeliveryNotificationViewedEvent.Payload PAYLOAD = PnDeliveryNotificationViewedEvent.Payload.builder()
            .iun("iun_123")
            .build();

    private static final PnDeliveryNotificationViewedEvent.Payload PAYLOAD_WITH_DELEGATE = PnDeliveryNotificationViewedEvent.Payload.builder()
            .iun("iun_123")
            .delegateInfo(NotificationViewDelegateInfo.builder()
                    .internalId("delegate_123")
                    .delegateType(NotificationViewDelegateInfo.DelegateType.PF)
                    .build())
            .build();

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_VIEWED, handler.getSupportedEventType());
    }

    @Test
    void getPayloadTypeReturnsCorrectType() {
        assertEquals(PnDeliveryNotificationViewedEvent.Payload.class, handler.getPayloadType());
    }

    @Test
    void handleExecutes() {
        handler.handle(PAYLOAD_WITH_DELEGATE, headers);

        Mockito.verify(notificationViewedRequestHandler).handleViewNotificationDelivery(Mockito.any());
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Mockito.doThrow(new RuntimeException("Validation error")).when(notificationViewedRequestHandler).handleViewNotificationDelivery(Mockito.any());

        assertThrows(RuntimeException.class, () -> handler.handle(PAYLOAD, headers));

        Mockito.verify(notificationViewedRequestHandler).handleViewNotificationDelivery(Mockito.any());
    }
}