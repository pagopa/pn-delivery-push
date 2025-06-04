package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypush.action.notificationpaid.NotificationPaidHandler;
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
class CustomNotificationPaidEventHandlerTest {
    @Mock
    private NotificationPaidHandler notificationPaidHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private CustomNotificationPaidEventHandler handler;

    private static final PnDeliveryPaymentEvent.Payload PAYLOAD = PnDeliveryPaymentEvent.Payload.builder()
            .iun("iun_123")
            .recipientIdx(1)
            .build();

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_PAID, handler.getSupportedEventType());
    }

    @Test
    void getPayloadTypeReturnsCorrectType() {
        assertEquals(PnDeliveryPaymentEvent.Payload.class, handler.getPayloadType());
    }

    @Test
    void handleExecutes() {
        handler.handle(PAYLOAD, headers);

        Mockito.verify(notificationPaidHandler).handleNotificationPaid(PAYLOAD);
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Mockito.doThrow(new RuntimeException("Validation error")).when(notificationPaidHandler).handleNotificationPaid(PAYLOAD);

        assertThrows(RuntimeException.class, () -> handler.handle(PAYLOAD, headers));

        Mockito.verify(notificationPaidHandler).handleNotificationPaid(PAYLOAD);
    }

}