package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.MockActionPoolTest;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationpaid.NotificationPaidInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationPaidMapper;
import it.pagopa.pn.deliverypush.utils.UUIDCreatorUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.config.RoutingFunction;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@FunctionalSpringBootTest
@Import(LocalStackTestConfig.class)
class NotificationPaidEventHandlerTestIT extends MockActionPoolTest{

    @Autowired
    private FunctionCatalog functionCatalog;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimelineService timelineService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private TimelineUtils timelineUtils;

    @MockBean
    private UUIDCreatorUtils uuidCreatorUtils;
    
    @MockBean
    private NotificationProcessCostService notificationProcessCostService;

    @Test
    void consumeMessageWithPaymentPPANotAlreadyPaid() {
        final String IUN = "iun-value-12345-6789";
        final String CREDITOR_TAX_ID = "77777777777"; //stringa di 11 caratteri
        final String NOTICE_CODE = "123456789123456789"; //stringa di 18 caratteri
        final PnDeliveryPaymentEvent.PaymentType PAYMENT_TYPE = PnDeliveryPaymentEvent.PaymentType.PAGOPA;
        final String ELEMENT_ID_EXPECTED = "NOTIFICATION_PAID.IUN_" + IUN + ".CODE_PPA" + NOTICE_CODE + CREDITOR_TAX_ID;

        NotificationInt notificationIntMock = buildNotification(IUN);
        Message<PnDeliveryPaymentEvent.Payload> payloadMessage = buildMessage(IUN, CREDITOR_TAX_ID, NOTICE_CODE, PAYMENT_TYPE);
        NotificationPaidInt notificationPaidInt = NotificationPaidMapper.messageToInternal(payloadMessage.getPayload());
        TimelineElementInternal timelineElementInternalExpected = new TimelineUtils(null, null, notificationProcessCostService).buildNotificationPaidTimelineElement(notificationIntMock, notificationPaidInt, ELEMENT_ID_EXPECTED);

        when(notificationProcessCostService.getSendFee()).thenReturn(100);
        when(timelineUtils.buildNotificationPaidTimelineElement(notificationIntMock, notificationPaidInt, ELEMENT_ID_EXPECTED)).thenReturn(timelineElementInternalExpected);
        when(timelineService.getTimelineElement(anyString(), anyString())).thenReturn(Optional.empty());
        when(notificationService.getNotificationByIun(IUN)).thenReturn(notificationIntMock);
        Function<Object, Object> function = functionCatalog.lookup(RoutingFunction.FUNCTION_NAME);

        // verifico che il messaggio venga correttamente gestito dal router grazie all'eventType
        assertThat(function).isNotNull();
        // verifico che il flusso da quando arriva il messaggio al salvataggio della timeline non provochi eccezioni
        assertDoesNotThrow(() -> function.apply(payloadMessage));
        // verifico che viene eseguito il metodo di aggiunta della timeline con id ELEMENT_ID_EXPECTED
        verify(timelineService, times(1)).addTimelineElement(timelineElementInternalExpected, notificationIntMock);
    }

    @Test
    void consumeMessageWithPaymentPPAlreadyPaid() {
        final String IUN = "iun-value-12345-6789";
        final String CREDITOR_TAX_ID = "77777777777"; //stringa di 11 caratteri
        final String NOTICE_CODE = "123456789123456789"; //stringa di 18 caratteri
        final PnDeliveryPaymentEvent.PaymentType PAYMENT_TYPE = PnDeliveryPaymentEvent.PaymentType.PAGOPA;
        final String ELEMENT_ID_EXPECTED = "NOTIFICATION_PAID.IUN_" + IUN + ".CODE_PPA" + NOTICE_CODE + CREDITOR_TAX_ID;

        NotificationInt notificationIntMock = buildNotification(IUN);
        Message<PnDeliveryPaymentEvent.Payload> payloadMessage = buildMessage(IUN, CREDITOR_TAX_ID, NOTICE_CODE, PAYMENT_TYPE);
        NotificationPaidInt notificationPaidInt = NotificationPaidMapper.messageToInternal(payloadMessage.getPayload());
        TimelineElementInternal timelineElementInternalExpected = new TimelineUtils(null, null, notificationProcessCostService).buildNotificationPaidTimelineElement(notificationIntMock, notificationPaidInt, ELEMENT_ID_EXPECTED);

        when(timelineUtils.buildNotificationPaidTimelineElement(notificationIntMock, notificationPaidInt, ELEMENT_ID_EXPECTED)).thenReturn(timelineElementInternalExpected);
        when(timelineService.getTimelineElement(anyString(), anyString())).thenReturn(Optional.of(TimelineElementInternal.builder().build()));
        when(notificationService.getNotificationByIun(IUN)).thenReturn(notificationIntMock);
        Function<Object, Object> function = functionCatalog.lookup(RoutingFunction.FUNCTION_NAME);

        // verifico che il messaggio venga correttamente gestito dal router grazie all'eventType
        assertThat(function).isNotNull();
        // verifico che il flusso da quando arriva il messaggio al (NON) salvataggio della timeline non provochi eccezioni
        assertDoesNotThrow(() -> function.apply(payloadMessage));
        // verifico che NON viene eseguito il metodo di aggiunta della timeline con id ELEMENT_ID_EXPECTED, perché già pagata
        verify(timelineService, times(0)).addTimelineElement(timelineElementInternalExpected, notificationIntMock);
    }

    private Message<PnDeliveryPaymentEvent.Payload> buildMessage(String iun, String creditorTaxId, String noticeCode,
                                                                 PnDeliveryPaymentEvent.PaymentType paymentType) {

        int recipientIdx = 0;
        String eventId = iun + "_notification_paid_" + recipientIdx;
        PnDeliveryPaymentEvent paymentEvent = PnDeliveryPaymentEvent.builder()
                .messageDeduplicationId(eventId)
                .messageGroupId("delivery")
                .header(StandardEventHeader.builder()
                        .iun(iun)
                        .eventId(eventId)
                        .createdAt(Instant.now())
                        .eventType(EventType.NOTIFICATION_PAID.name())
                        .publisher(EventPublisher.DELIVERY.name())
                        .build()
                )
                .payload(PnDeliveryPaymentEvent.Payload.builder()
                        .iun(iun)
                        .paymentType(paymentType)
                        .paymentDate(Instant.now())
                        .uncertainPaymentDate(true)
                        .recipientIdx(recipientIdx)
                        .recipientType(PnDeliveryPaymentEvent.RecipientType.PF)
                        .creditorTaxId(creditorTaxId)
                        .noticeCode(noticeCode)
                        .paymentSourceChannel("PA")
                        .amount(1000)
                        .build()
                )
                .build();

        Message<PnDeliveryPaymentEvent.Payload> message = MessageBuilder
                .createMessage(paymentEvent.getPayload(), new MessageHeaders(objectMapper.convertValue(paymentEvent.getHeader(), Map.class)));

        return message;
    }

    private NotificationInt buildNotification(String iun) {
        return NotificationInt.builder()
                .iun(iun)
                .recipients(List.of(
                        NotificationRecipientInt.builder().build()
                ))
                .sender(NotificationSenderInt.builder().paId("77777777777").build())
                .sentAt(Instant.now())
                .build();
    }

}
