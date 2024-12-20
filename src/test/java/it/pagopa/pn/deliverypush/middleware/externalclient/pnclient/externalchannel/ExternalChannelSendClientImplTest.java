package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.api.DigitalLegalMessagesApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.api.PaperMessagesApi;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalChannelSendClientImplTest {
    
    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DigitalLegalMessagesApi digitalLegalMessagesApi;

    @Mock
    private DigitalCourtesyMessagesApi digitalCourtesyMessagesApi;

    @Mock
    private PaperMessagesApi paperMessagesApi;

    @Mock
    private LegalFactGenerator legalFactGenerator;


    private ExternalChannelSendClientImpl client;

    @BeforeEach
    void setup() {
        this.cfg = mock(PnDeliveryPushConfigs.class);
        Mockito.when(cfg.getExternalChannelBaseUrl()).thenReturn("http://localhost:8080");
        Mockito.when(cfg.getExternalchannelCxId()).thenReturn("pn-delivery-002");
        
        client = new ExternalChannelSendClientImpl(cfg, digitalLegalMessagesApi, digitalCourtesyMessagesApi, legalFactGenerator);
    }


    @ParameterizedTest
    @CsvSource({"test@dominioPec.it, PEC", "x-pagopa-pn-sercq:send-self:notification-already-delivered, SERCQ"})
    @ExtendWith(SpringExtension.class)
    void sendLegalNotification(String address, LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType) {
        NotificationInt notificationInt = buildNotification();
        NotificationRecipientInt notificationRecipientInt = buildNotificationRecipientInt(address, channelType);
        LegalDigitalAddressInt legalDigitalAddressInt = buildLegalDigitalAddressInt(address, channelType);
        String timelineEventId = "001";
        String aarKey = "testKey";
        String quickAccessToken = "test";

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        assertDoesNotThrow(() -> client.sendLegalNotification(notificationInt, notificationRecipientInt, legalDigitalAddressInt, timelineEventId, Collections.singletonList(aarKey), quickAccessToken));
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void sendCourtesyNotificationEmail() {
        NotificationInt notificationInt = buildNotification();
        NotificationRecipientInt notificationRecipientInt = buildNotificationRecipientInt();
        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                //.address(courtesyDigitalAddress.getValue())
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                .build();

        String timelineEventId = "001";
        String aarKey = "testKey";

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        assertDoesNotThrow(() -> client.sendCourtesyNotification(notificationInt, notificationRecipientInt, courtesyDigitalAddressInt, timelineEventId, aarKey, ""));
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void sendCourtesyNotificationSms() {
        NotificationInt notificationInt = buildNotification();
        NotificationRecipientInt notificationRecipientInt = buildNotificationRecipientInt();
        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                //.address(courtesyDigitalAddress.getValue())
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
                .build();

        String timelineEventId = "001";
        String aarKey = "testKey";

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));

        assertDoesNotThrow(() -> client.sendCourtesyNotification(notificationInt, notificationRecipientInt, courtesyDigitalAddressInt, timelineEventId, aarKey, ""));
    }

    @ParameterizedTest
    @ExtendWith(SpringExtension.class)
    @CsvSource({"test@dominioPec.it, PEC", "x-pagopa-pn-sercq:send-self:notification-already-delivered, SERCQ"})
    void sendLegalNotificationPEC(String address, LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType) {

        //Given
        NotificationInt notificationInt = mock(NotificationInt.class);
        NotificationRecipientInt recipientInt = mock(NotificationRecipientInt.class);
        LegalDigitalAddressInt addressInt = mock(LegalDigitalAddressInt.class);
        String eventId = "rtyuiokjhgvcbnjmk4567890";
        String aarKey = "testKey";
        String quickAccessToken = "test";

        Mockito.when(restTemplate.exchange(Mockito.any(RequestEntity.class), Mockito.any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(""));
        when(addressInt.getType()).thenReturn(channelType);
        when(addressInt.getAddress()).thenReturn(address);

        //When

        assertDoesNotThrow(() -> client.sendLegalNotification(notificationInt, recipientInt, addressInt, eventId, Collections.singletonList(aarKey), quickAccessToken));

    }

    private NotificationRecipientInt buildNotificationRecipientInt() {
        return NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination("Galileo Bruno")
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(new PhysicalAddressInt(
                        "Galileo Bruno",
                        "Palazzo dell'Inquisizione",
                        "corso Italia 666",
                        "Piano Terra (piatta)",
                        "00100",
                        "Roma",
                        null,
                        "RM",
                        "IT"
                ))
                .build();
    }

    private NotificationRecipientInt buildNotificationRecipientInt(String address, LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType) {
        return NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination("Galileo Bruno")
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address(address)
                        .type(channelType)
                        .build())
                .physicalAddress(new PhysicalAddressInt(
                        "Galileo Bruno",
                        "Palazzo dell'Inquisizione",
                        "corso Italia 666",
                        "Piano Terra (piatta)",
                        "00100",
                        "Roma",
                        null,
                        "RM",
                        "IT"
                ))
                .build();
    }

    private NotificationInt buildNotification() {
        return NotificationInt.builder()
                .sender(createSender())
                .sentAt(Instant.now())
                .iun("Example_IUN_1234_Test")
                .subject("notification test subject")
                .documents(Arrays.asList(
                                NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64Utils.encodeToString("sha256_doc01".getBytes())))
                                                .build()
                                        )
                                        .build()
                        )
                )
                .recipients(buildRecipients())
                .build();
    }

    private List<NotificationRecipientInt> buildRecipients() {
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination("Galileo Bruno")
                .digitalDomicile(buildLegalDigitalAddressInt("test@dominioPec.it", LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC))
                .physicalAddress(buildPhysicalAddressInt())
                .build();

        return Collections.singletonList(rec1);
    }

    private LegalDigitalAddressInt buildLegalDigitalAddressInt(String address, LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType) {
        return LegalDigitalAddressInt.builder()
                .address(address)
                .type(channelType)
                .build();
    }

    private PhysicalAddressInt buildPhysicalAddressInt() {
        return new PhysicalAddressInt(
                "Galileo Bruno",
                "Palazzo dell'Inquisizione",
                "corso Italia 666",
                "Piano Terra (piatta)",
                "00100",
                "Roma",
                null,
                "RM",
                "IT"
        );
    }

    private NotificationSenderInt createSender() {
        return NotificationSenderInt.builder()
                .paId("TEST_PA_ID")
                .paTaxId("TEST_TAX_ID")
                .paDenomination("TEST_PA_DENOMINATION")
                .build();
    }
}