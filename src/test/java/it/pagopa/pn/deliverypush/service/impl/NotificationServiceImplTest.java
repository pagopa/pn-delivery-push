package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

class NotificationServiceImplTest {

    @Mock
    private PnDeliveryClient pnDeliveryClient;

    @Mock
    private PnDeliveryClientReactive pnDeliveryClientReactive;

    private NotificationServiceImpl service;

    @BeforeEach
    public void setup() {
        service = new NotificationServiceImpl(pnDeliveryClient, pnDeliveryClientReactive);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIun() {
        NotificationInt expected = buildNotificationInt();

        SentNotification sentNotification = buildSentNotification();
        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenReturn(ResponseEntity.ok(sentNotification));

        NotificationInt actual = service.getNotificationByIun("001");

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIunNotFound() {

        String expectErrorMsg = "PN_DELIVERYPUSH_NOTIFICATIONFAILED";

        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenReturn(ResponseEntity.ok(null));

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            service.getNotificationByIun("001");
        });

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIunReactive() {
        NotificationInt expected = buildNotificationInt();
        it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.model.SentNotification sentNotification = buildSentNotificationReactive();
        Mockito.when(pnDeliveryClientReactive.getSentNotification("001")).thenReturn(Mono.just(sentNotification));

        Mono<NotificationInt> actual = service.getNotificationByIunReactive("001");

        Assertions.assertEquals(expected, actual.block());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIunNotFoundReactive() {

        String expectErrorMsg = "PN_DELIVERYPUSH_NOTIFICATIONFAILED";

        Mockito.when(pnDeliveryClientReactive.getSentNotification("001")).thenReturn(Mono.empty());

        service.getNotificationByIunReactive("001")
                .onErrorResume( error -> {
                    PnInternalException pnInternalException = (PnInternalException) error;
                    Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
                    return Mono.empty();
                });
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getRecipientsQuickAccessLinkToken() {
        Map<String, String> expected = Map.of("internalId","token");

        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001")).thenReturn(ResponseEntity.ok(expected));

        Map<String, String> actual = service.getRecipientsQuickAccessLinkToken("001");

        Assertions.assertEquals(expected, actual);
    }
    
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getRecipientsQuickAccessLinkTokenFailure() {
       
        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001"))
        .thenReturn(ResponseEntity.internalServerError().body(Map.of()));


        Assertions.assertThrows(PnInternalException.class, () -> {
          service.getRecipientsQuickAccessLinkToken("001");
      });
        
    }
    
    private SentNotification buildSentNotification() {
        SentNotification sentNotification = new SentNotification();
        sentNotification.setIun("001");
        sentNotification.setPhysicalCommunicationType(SentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        return sentNotification;
    }

    private it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.model.SentNotification buildSentNotificationReactive() {
        it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.model.SentNotification sentNotification = new it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.model.SentNotification();
        sentNotification.setIun("001");
        sentNotification.setPhysicalCommunicationType(it.pagopa.pn.delivery.generated.openapi.clients.delivery_reactive.model.SentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        return sentNotification;
    }
    
    private NotificationInt buildNotificationInt() {
        return NotificationInt.builder()
                .iun("001")
                .recipients(Collections.emptyList())
                .documents(Collections.emptyList())
                .sender(NotificationSenderInt.builder().build())
                .physicalCommunicationType(ServiceLevelTypeInt.REGISTERED_LETTER_890)
                .build();
    }
}