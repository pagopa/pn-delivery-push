package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry.PublicRegistry;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

class PublicRegistryServiceImplTest {

    @Mock
    private PublicRegistryUtils publicRegistryUtils;

    @Mock
    private PublicRegistry publicRegistry;

    @Mock
    private NotificationUtils notificationUtils;

    private PublicRegistryServiceImpl service;

    @BeforeEach
    void setUp() {
        publicRegistryUtils = Mockito.mock(PublicRegistryUtils.class);
        publicRegistry = Mockito.mock(PublicRegistry.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);

        service = new PublicRegistryServiceImpl(publicRegistryUtils, publicRegistry, notificationUtils);
    }

    @Test
    void sendRequestForGetDigitalGeneralAddress() {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        Integer recIndex = 1;
        ContactPhaseInt contactPhase = ContactPhaseInt.SEND_ATTEMPT;
        int sentAttemptMade = 1;
        NotificationRecipientInt recipient = buildRecipient(denomination);
        String correlationId = "001";

        Mockito.when(publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, contactPhase, sentAttemptMade, DeliveryModeInt.DIGITAL)).thenReturn(correlationId);
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndex)).thenReturn(recipient);

        service.sendRequestForGetDigitalGeneralAddress(notification, recIndex, contactPhase, sentAttemptMade);

        Mockito.verify(publicRegistryUtils, Mockito.times(1)).addPublicRegistryCallToTimeline(notification, recIndex, contactPhase, sentAttemptMade, correlationId, DeliveryModeInt.DIGITAL);
        Mockito.verify(publicRegistry, Mockito.times(1)).sendRequestForGetDigitalAddress(recipient.getTaxId(), correlationId);
    }

    @Test
    void sendRequestForGetPhysicalAddress() {
        String denomination = "<h1>SSRF WITH IMAGE POC</h1> <img src='https://prova.it'></img>";
        NotificationInt notification = buildNotification(denomination);
        Integer recIndex = 1;
        int sentAttemptMade = 1;
        String correlationId = "001";
        NotificationRecipientInt recipient = buildRecipient(denomination);

        Mockito.when(publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, ContactPhaseInt.SEND_ATTEMPT, sentAttemptMade, DeliveryModeInt.ANALOG)).thenReturn(correlationId);
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndex)).thenReturn(recipient);

        service.sendRequestForGetPhysicalAddress(notification, recIndex, sentAttemptMade);

        Mockito.verify(publicRegistryUtils, Mockito.times(1)).addPublicRegistryCallToTimeline(notification, recIndex, ContactPhaseInt.SEND_ATTEMPT, sentAttemptMade, correlationId, DeliveryModeInt.ANALOG);
        Mockito.verify(publicRegistry, Mockito.times(1)).sendRequestForGetPhysicalAddress(recipient.getTaxId(), correlationId);

    }

    private NotificationInt buildNotification(String denomination) {
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
                .recipients(Collections.singletonList(buildRecipient(denomination)))
                .build();
    }

    private NotificationRecipientInt buildRecipient(String denomination) {
        String defaultDenomination = StringUtils.hasText(denomination) ? denomination : "Galileo Bruno";
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination(defaultDenomination)
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(buildPhysicalAddressInt())
                .build();

        return rec1;
    }

    private PhysicalAddressInt buildPhysicalAddressInt() {
        return new PhysicalAddressInt(
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