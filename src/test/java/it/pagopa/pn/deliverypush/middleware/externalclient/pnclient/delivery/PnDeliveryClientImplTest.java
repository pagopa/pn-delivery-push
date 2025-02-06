package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.api.InternalOnlyApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV24;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

class PnDeliveryClientImplTest {

    @Mock
    private InternalOnlyApi pnDeliveryApi;

    private PnDeliveryClientImpl client;

    @BeforeEach
    void setup() {
        client = new PnDeliveryClientImpl(pnDeliveryApi);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getSentNotification() {
        SentNotificationV24 notification = new SentNotificationV24();
        notification.setIun("001");
        
        Mockito.when(pnDeliveryApi.getSentNotificationPrivateWithHttpInfo("001")).thenReturn(ResponseEntity.ok(notification));

        SentNotificationV24 res = client.getSentNotification("001");

        Assertions.assertEquals("001", res.getIun());

    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getQuickAccessLinkTokensPrivate() {
       Map<String, String> expected = Map.of("internalId","token");
        
        Mockito.when(pnDeliveryApi.getQuickAccessLinkTokensPrivateWithHttpInfo("001")).thenReturn(ResponseEntity.ok(expected));

        Map<String, String> res = client.getQuickAccessLinkTokensPrivate("001");

        Assertions.assertEquals(expected, res);
    }
}