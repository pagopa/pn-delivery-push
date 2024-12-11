package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.it.CommonTestConfiguration;
import it.pagopa.pn.deliverypush.action.it.mockbean.TemplatesClientMock;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.legalfacts.AARInfo;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

@SpringBootTest
@TestPropertySource(properties = "pn.delivery-push.enable-templates-engine=true")
class LegalFactGeneratorTemplatesTest extends CommonTestConfiguration {

    @Autowired
    LegalFactGenerator legalFactGeneratorTemplatesTest;
    @MockBean
    TemplatesClient templatesClient;

    TemplatesClientMock templatesClientMock = new TemplatesClientMock();

    private static final String IUN = "TEST_TEST";
    private static final String TEST_RETURN = "Templates As String Result";
    public static final String QUICK_ACCESS_TOKEN = "quickAccessToken_TEST";

    @Test
    void generateNotificationReceivedLegalFact() {
        Mockito.when(templatesClient.notificationReceivedLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationReceivedLegalFact.class)))
                .thenReturn(templatesClientMock.notificationReceivedLegalFact(LanguageEnum.IT, new NotificationReceivedLegalFact()));
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorTemplatesTest.generateNotificationReceivedLegalFact(notificationInt()));
        Assertions.assertNotNull(result);
    }

    @Test
    void generateNotificationViewedLegalFact() {
        Mockito.when(templatesClient.notificationViewedLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationViewedLegalFact.class)))
                .thenReturn(templatesClientMock.notificationViewedLegalFact(LanguageEnum.IT, new NotificationViewedLegalFact()));
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorTemplatesTest.generateNotificationViewedLegalFact(
                        IUN, notificationRecipientInt(), delegateInfoInt(), Instant.now(), notificationInt()
                ));
        Assertions.assertNotNull(result);
    }

    @Test
    void generatePecDeliveryWorkflowLegalFact() {
        Mockito.when(templatesClient.pecDeliveryWorkflowLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(PecDeliveryWorkflowLegalFact.class)))
                .thenReturn(templatesClientMock.pecDeliveryWorkflowLegalFact(LanguageEnum.IT, new PecDeliveryWorkflowLegalFact()));
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorTemplatesTest.generatePecDeliveryWorkflowLegalFact(
                        List.of(sendDigitalFeedbackDetailsInt()), notificationInt(), notificationRecipientInt(), EndWorkflowStatus.SUCCESS, Instant.now()
                ));
        Assertions.assertNotNull(result);
    }

    @Test
    void generateAnalogDeliveryFailureWorkflowLegalFact() {
        Mockito.when(templatesClient.analogDeliveryWorkflowFailureLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(AnalogDeliveryWorkflowFailureLegalFact.class)))
                .thenReturn(templatesClientMock.analogDeliveryWorkflowFailureLegalFact(LanguageEnum.IT, new AnalogDeliveryWorkflowFailureLegalFact()));
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorTemplatesTest.generateAnalogDeliveryFailureWorkflowLegalFact(
                        notificationInt(), notificationRecipientInt(), EndWorkflowStatus.SUCCESS, Instant.now()
                ));
        Assertions.assertNotNull(result);
    }

    @Test
    void generateNotificationCancelledLegalFact() {
        Mockito.when(templatesClient.notificationCancelledLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationCancelledLegalFact.class)))
                .thenReturn(templatesClientMock.notificationCancelledLegalFact(LanguageEnum.IT, new NotificationCancelledLegalFact()));
        var result = Assertions.assertDoesNotThrow(() ->
                legalFactGeneratorTemplatesTest.generateNotificationCancelledLegalFact(notificationInt(), Instant.now()));
        Assertions.assertNotNull(result);
    }

    @Test
    void generateNotificationAARSubject() {
        Mockito.when(templatesClient.notificationAarForSubject(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForSubject.class)))
                .thenReturn(templatesClientMock.notificationAarForSubject(LanguageEnum.IT, new NotificationAarForSubject()));
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorTemplatesTest.generateNotificationAARSubject(notificationInt()));
        Assertions.assertEquals(TEST_RETURN, result);
    }

    @Test
    void generateNotificationAAR_CASE_NOTIFICATION_AAR() {
        Mockito.when(templatesClient.notificationAar(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAar.class)))
                .thenReturn(templatesClientMock.notificationAar(LanguageEnum.IT, new NotificationAar()));

        AARInfo result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorTemplatesTest.generateNotificationAAR(
                notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN)
        );
        Assertions.assertNotNull(result.getBytesArrayGeneratedAar());
    }

    @Test
    void generateNotificationAARBody() {
        Mockito.when(templatesClient.notificationAarForEmail(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForEmail.class)))
                .thenReturn(templatesClientMock.notificationAarForEmail(LanguageEnum.IT, new NotificationAarForEmail()));
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorTemplatesTest.generateNotificationAARBody(
                notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN));
        Assertions.assertEquals(TEST_RETURN, result);
    }

    @Test
    void generateNotificationAARPECBody() {
        Mockito.when(templatesClient.notificationAarForPec(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForPec.class)))
                .thenReturn(templatesClientMock.notificationAarForPec(LanguageEnum.IT, new NotificationAarForPec()));
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorTemplatesTest.generateNotificationAARPECBody(
                notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN));
        Assertions.assertEquals(TEST_RETURN, result);
    }

    @Test
    void generateNotificationAARForSMS() {
        Mockito.when(templatesClient.notificationAarForSms(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForSms.class)))
                .thenReturn(templatesClientMock.notificationAarForSms(LanguageEnum.IT, new NotificationAarForSms()));
        var result = Assertions.assertDoesNotThrow(() -> legalFactGeneratorTemplatesTest.generateNotificationAARForSMS(notificationInt()));
        Assertions.assertEquals(TEST_RETURN, result);
    }

    private static SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetailsInt() {
        return SendDigitalFeedbackDetailsInt.builder()
                .recIndex(10)
                .digitalAddress(legalDigitalAddressInt())
                .notificationDate(Instant.now())
                .requestTimelineId("requestTimelineId_TEST")
                .build();
    }

    private static DelegateInfoInt delegateInfoInt() {
        return DelegateInfoInt.builder()
                .taxId("DelegateInfoInt_TAX_ID")
                .build();
    }

    private static NotificationInt notificationInt() {
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

    private static NotificationRecipientInt notificationRecipientInt() {
        return NotificationRecipientInt.builder()
                .denomination("denomination_test")
                .taxId("taxId_test_test")
                .recipientType(RecipientTypeInt.PF)
                .physicalAddress(physicalAddressInt())
                .digitalDomicile(legalDigitalAddressInt())
                .build();
    }

    private static PhysicalAddressInt physicalAddressInt() {
        return PhysicalAddressInt.builder()
                .zip("00000")
                .build();
    }

    private static LegalDigitalAddressInt legalDigitalAddressInt() {
        return LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .address("DigitalAddress_TEST")
                .build();
    }

    private static NotificationDocumentInt notificationDocumentInt() {
        return NotificationDocumentInt.builder()
                .contentType("PDF_TEST_TEST")
                .digests(NotificationDocumentInt.Digests.builder()
                        .sha256("string")
                        .build())
                .build();
    }

    private static NotificationSenderInt notificationSenderInt() {
        return NotificationSenderInt.builder()
                .paDenomination("paDenomination_TEST_TEST")
                .paTaxId("paTaxId_TEST_TEST")
                .build();
    }
}