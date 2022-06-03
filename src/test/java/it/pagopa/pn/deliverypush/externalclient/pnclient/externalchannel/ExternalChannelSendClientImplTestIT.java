package it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.api.PaperMessagesApi;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ExternalChannelSendClientImplTestIT {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private PaperMessagesApi paperMessagesApi;

    @Mock
    private LegalFactGenerator legalFactGenerator;

    private ExternalChannelSendClientImpl externalChannelSendClient;

    @BeforeEach
    void setup() {
        this.cfg = mock( PnDeliveryPushConfigs.class );
        Mockito.when( cfg.getExternalChannelBaseUrl() ).thenReturn( "http://localhost:8080" );
        Mockito.when( cfg.getExternalchannelCxId() ).thenReturn( "pn-delivery-002" );
        this.externalChannelSendClient = new ExternalChannelSendClientImpl( restTemplate, cfg, legalFactGenerator);
        this.externalChannelSendClient.init();
    }

    @Test
    void sendAnalogNotification() {

        //Given
        NotificationInt notificationInt = mock(NotificationInt.class);
        NotificationRecipientInt recipientInt = mock(NotificationRecipientInt.class);
        PhysicalAddress physicalAddress = mock(PhysicalAddress.class);
        String eventid = "rtyuiokjhgvcbnjmk4567890";

        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( ResponseEntity.ok("") );
        when(notificationInt.getSender()).thenReturn(new NotificationSenderInt());

        //When

        assertDoesNotThrow(() -> externalChannelSendClient.sendAnalogNotification( notificationInt, recipientInt, physicalAddress, eventid, ExternalChannelSendClient.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER, "frtghyuiugfdfghj" ));

        //Then

    }

    @Test
    void sendLegalNotificationPEC() {

        //Given
        NotificationInt notificationInt = mock(NotificationInt.class);
        NotificationRecipientInt recipientInt = mock(NotificationRecipientInt.class);
        LegalDigitalAddressInt addressInt = mock(LegalDigitalAddressInt.class);
        String eventid = "rtyuiokjhgvcbnjmk4567890";

        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( ResponseEntity.ok("") );
        when(notificationInt.getSender()).thenReturn(new NotificationSenderInt());
        when(addressInt.getType()).thenReturn(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC);
        when(addressInt.getAddress()).thenReturn("email@email.it");

        //When

        assertDoesNotThrow(() -> externalChannelSendClient.sendLegalNotification( notificationInt, recipientInt, addressInt, eventid));

    }

    @Test
    void sendCourtesyNotificationEMAIL() {
        //Given
        NotificationInt notificationInt = mock(NotificationInt.class);
        NotificationRecipientInt recipientInt = mock(NotificationRecipientInt.class);
        CourtesyDigitalAddressInt addressInt = mock(CourtesyDigitalAddressInt.class);
        String eventid = "rtyuiokjhgvcbnjmk4567890";

        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( ResponseEntity.ok("") );
        when(notificationInt.getSender()).thenReturn(new NotificationSenderInt());
        when(addressInt.getType()).thenReturn(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE.EMAIL);
        when(addressInt.getAddress()).thenReturn("email@email.it");

        //When

        assertDoesNotThrow(() -> externalChannelSendClient.sendCourtesyNotification( notificationInt, recipientInt, addressInt, eventid));

    }

    @Test
    void sendCourtesyNotificationSMS() {
        //Given
        NotificationInt notificationInt = mock(NotificationInt.class);
        NotificationRecipientInt recipientInt = mock(NotificationRecipientInt.class);
        CourtesyDigitalAddressInt addressInt = mock(CourtesyDigitalAddressInt.class);
        String eventid = "rtyuiokjhgvcbnjmk4567890";

        Mockito.when( restTemplate.exchange( Mockito.any(RequestEntity.class),Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn( ResponseEntity.ok("") );
        when(notificationInt.getSender()).thenReturn(new NotificationSenderInt());
        when(addressInt.getType()).thenReturn(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE.SMS);

        //When

        assertDoesNotThrow(() -> externalChannelSendClient.sendCourtesyNotification( notificationInt, recipientInt, addressInt, eventid));

    }
}