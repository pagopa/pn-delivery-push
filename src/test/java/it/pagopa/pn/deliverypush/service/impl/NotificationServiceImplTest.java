package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV24;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
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

        SentNotificationV24 sentNotification = buildSentNotification();
        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenReturn(sentNotification);

        NotificationInt actual = service.getNotificationByIun("001");

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIunNotFound() {

        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenThrow(PnHttpResponseException.class);

        Assertions.assertThrows(PnHttpResponseException.class, () -> service.getNotificationByIun("001"));

    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getNotificationByIunReactive() {
        NotificationInt expected = buildNotificationInt();
        SentNotificationV24 sentNotification = buildSentNotificationReactive();
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

        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001")).thenReturn(expected);

        Map<String, String> actual = service.getRecipientsQuickAccessLinkToken("001");

        Assertions.assertEquals(expected, actual);
    }
    
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getRecipientsQuickAccessLinkTokenFailure() {       
        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001"))
        .thenThrow(PnHttpResponseException.class);
        Assertions.assertThrows(PnHttpResponseException.class, () -> service.getRecipientsQuickAccessLinkToken("001"));
        
    }



    @Test
    @ExtendWith(SpringExtension.class)
    void removeAllNotificationCostsByIun() {
        Mockito.when(pnDeliveryClientReactive.removeAllNotificationCostsByIun("001"))
                .thenReturn(Mono.empty());

        Mono<Void> mono = service.removeAllNotificationCostsByIun("001");
        Assertions.assertDoesNotThrow( () -> mono.block());

    }

    @Test
    @ExtendWith(SpringExtension.class)
    void removeAllNotificationCostsByIunError() {
        Mockito.when(pnDeliveryClientReactive.removeAllNotificationCostsByIun("001"))
                .thenReturn(Mono.error(new PnHttpResponseException("", 400)));

        Mono<Void> mono = service.removeAllNotificationCostsByIun("001");
        Assertions.assertThrows(PnInternalException.class, mono::block);

    }



    @Test
    @ExtendWith(SpringExtension.class)
    void updateStatus() {
        Mockito.when(pnDeliveryClientReactive.updateStatus(Mockito.eq("001"), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mono<Void> mono = service.updateStatus("001", NotificationStatusInt.CANCELLED, Instant.EPOCH);
        Assertions.assertDoesNotThrow( () -> mono.block());

    }

    @Test
    @ExtendWith(SpringExtension.class)
    void updateStatusError() {
        Mockito.when(pnDeliveryClientReactive.updateStatus( Mockito.eq("001"), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new PnHttpResponseException("", 400)));

        Mono<Void> mono = service.updateStatus("001", NotificationStatusInt.CANCELLED, Instant.EPOCH);
        Assertions.assertThrows(PnInternalException.class, mono::block);

    }
    
    private SentNotificationV24 buildSentNotification() {
        SentNotificationV24 sentNotification = new SentNotificationV24();
        sentNotification.setIun("001");
        sentNotification.setPhysicalCommunicationType(SentNotificationV24.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        sentNotification.setNotificationFeePolicy(it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationFeePolicy.DELIVERY_MODE);
        return sentNotification;
    }

    private SentNotificationV24 buildSentNotificationReactive() {
        SentNotificationV24 sentNotification = new SentNotificationV24();
        sentNotification.setIun("001");
        sentNotification.setPhysicalCommunicationType(SentNotificationV24.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        sentNotification.setNotificationFeePolicy(it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationFeePolicy.DELIVERY_MODE);
        return sentNotification;
    }
    
    private NotificationInt buildNotificationInt() {
        return NotificationInt.builder()
                .iun("001")
                .recipients(Collections.emptyList())
                .documents(Collections.emptyList())
                .sender(NotificationSenderInt.builder().build())
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .physicalCommunicationType(ServiceLevelTypeInt.REGISTERED_LETTER_890)
                .build();
    }
}