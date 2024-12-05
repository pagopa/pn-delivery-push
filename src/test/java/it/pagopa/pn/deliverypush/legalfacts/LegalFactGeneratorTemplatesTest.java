package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

@SpringBootTest
@TestPropertySource(value = "classpath:/application-testIT.properties",
        properties = "pn.delivery-push.enableTemplatesEngine=true")
class LegalFactGeneratorTemplatesTest {

    @SpyBean
    LegalFactGenerator legalFactGeneratorDocComposition;
    @MockBean
    TemplatesClientImpl templatesClient;

    private static final byte[] byteArray = {10, 20, 30, 40, 50};
    private static final String IUN = "TEST_TEST";
    private static final String TEST_RETURN = "AARSubject_ForSMS_PECBody";
    public static final String QUICK_ACCESS_TOKEN = "quickAccessToken_TEST";

    @Test
    void generateNotificationReceivedLegalFact() {
        Mockito.when(templatesClient.notificationReceivedLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationReceivedLegalFact.class)))
                .thenReturn(byteArray);
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorDocComposition.generateNotificationReceivedLegalFact(notificationInt()));
        Assertions.assertEquals(byteArray, result);
    }

    @Test
    void generateNotificationViewedLegalFact() {
        Mockito.when(templatesClient.notificationViewedLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationViewedLegalFact.class)))
                .thenReturn(byteArray);
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorDocComposition.generateNotificationViewedLegalFact(
                        IUN, notificationRecipientInt(), delegateInfoInt(), Instant.now(), notificationInt()
                ));
        Assertions.assertEquals(byteArray, result);
    }

    @Test
    void generatePecDeliveryWorkflowLegalFact() {
        Mockito.when(templatesClient.pecDeliveryWorkflowLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(PecDeliveryWorkflowLegalFact.class)))
                .thenReturn(byteArray);
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorDocComposition.generatePecDeliveryWorkflowLegalFact(
                        List.of(sendDigitalFeedbackDetailsInt()), notificationInt(), notificationRecipientInt(), EndWorkflowStatus.SUCCESS, Instant.now()
                ));
        Assertions.assertEquals(byteArray, result);
    }

    @Test
    void generateAnalogDeliveryFailureWorkflowLegalFact() {
        Mockito.when(templatesClient.analogDeliveryWorkflowFailureLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(AnalogDeliveryWorkflowFailureLegalFact.class)))
                .thenReturn(byteArray);
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorDocComposition.generateAnalogDeliveryFailureWorkflowLegalFact(
                        notificationInt(), notificationRecipientInt(), EndWorkflowStatus.SUCCESS, Instant.now()
                ));
        Assertions.assertEquals(byteArray, result);
    }

    @Test
    void generateNotificationCancelledLegalFact() {
        Mockito.when(templatesClient.notificationCancelledLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationCancelledLegalFact.class)))
                .thenReturn(byteArray);
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorDocComposition.generateNotificationCancelledLegalFact(notificationInt(), Instant.now()));
        Assertions.assertEquals(byteArray, result);
    }

    @Test
    void generateNotificationAARSubject() {
        Mockito.when(templatesClient.notificationAarForSubject(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForSubject.class)))
                .thenReturn(TEST_RETURN);
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorDocComposition.generateNotificationAARSubject(notificationInt()));
        Assertions.assertEquals(TEST_RETURN, result);
    }

    @Test
    void generateNotificationAAR_CASE_NOTIFICATION_AAR_ALT() {
        Mockito.when(templatesClient.notificationAarRaddAlt(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarRaddAlt.class)))
                .thenReturn(byteArray);
        AARInfo result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorDocComposition.generateNotificationAAR(
                notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN)
        );
        Assertions.assertEquals(byteArray, result.getBytesArrayGeneratedAar());
    }

    @Test
    void generateNotificationAARBody() {
        Mockito.when(templatesClient.notificationAarForEmail(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForEmail.class)))
                .thenReturn(TEST_RETURN);
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorDocComposition.generateNotificationAARBody(
                notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN));
        Assertions.assertEquals(TEST_RETURN, result);
    }

    @Test
    void generateNotificationAARPECBody() {
        Mockito.when(templatesClient.notificationAarForPec(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForPec.class)))
                .thenReturn(TEST_RETURN);
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorDocComposition.generateNotificationAARPECBody(
                notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN));
        Assertions.assertEquals(TEST_RETURN, result);
    }

    @Test
    void generateNotificationAARForSMS() {
        Mockito.when(templatesClient.notificationAarForSms(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForSms.class)))
                .thenReturn(TEST_RETURN);
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorDocComposition.generateNotificationAARForSMS(notificationInt()));
        Assertions.assertEquals(TEST_RETURN, result);
    }


    private SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetailsInt() {
        return SendDigitalFeedbackDetailsInt.builder()
                .recIndex(10)
                .digitalAddress(legalDigitalAddressInt())
                .notificationDate(Instant.now())
                .requestTimelineId("requestTimelineId_TEST")
                .build();
    }

    private DelegateInfoInt delegateInfoInt() {
        return DelegateInfoInt.builder()
                .taxId("DelegateInfoInt_TAX_ID")
                .build();
    }

    private NotificationInt notificationInt() {
        return NotificationInt.builder()
                .iun("TEST_TEST")
                .recipients(List.of(notificationRecipientInt()))
                .subject("subject_test")
                .sentAt(Instant.now())
                .sender(notificationSenderInt())
                .sentAt(Instant.now())
                .pagoPaIntMode(PagoPaIntMode.NONE)
                .documents(List.of(notificationDocumentInt()))
                .build();
    }

    private NotificationRecipientInt notificationRecipientInt() {
        return NotificationRecipientInt.builder()
                .denomination("denomination_test")
                .taxId("taxId_test_test")
                .recipientType(RecipientTypeInt.PF)
                .physicalAddress(physicalAddressInt())
                .digitalDomicile(legalDigitalAddressInt())
                .build();
    }

    private PhysicalAddressInt physicalAddressInt() {
        return PhysicalAddressInt.builder()
                .zip("00000")
                .build();
    }

    private LegalDigitalAddressInt legalDigitalAddressInt() {
        return LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .address("DigitalAddress_TEST")
                .build();
    }

    private NotificationDocumentInt notificationDocumentInt() {
        return NotificationDocumentInt.builder()
                .contentType("PDF_TEST_TEST")
                .digests(NotificationDocumentInt.Digests.builder()
                        .sha256("string")
                        .build())
                .build();
    }

    private NotificationSenderInt notificationSenderInt() {
        return NotificationSenderInt.builder()
                .paDenomination("paDenomination_TEST_TEST")
                .paTaxId("paTaxId_TEST_TEST")
                .build();
    }
}