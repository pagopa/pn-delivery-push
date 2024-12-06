package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.it.CommonTestConfiguration;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.templatesengine.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.templatesengine.TemplatesClientImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

//@SpringBootTest
//@TestPropertySource(properties = "pn.delivery-push.enableTemplatesEngine=true")
class LegalFactGeneratorTemplatesTest //extends CommonTestConfiguration
{

    @Autowired
    LegalFactGenerator legalFactGeneratorTemplatesTest;
    @MockBean
    TemplatesClientImpl templatesClient;

    private static final byte[] byteArray = {10, 20, 30, 40, 50};
    private static final String IUN = "TEST_TEST";
    private static final String TEST_RETURN = "AARSubject_ForSMS_PECBody";
    public static final String QUICK_ACCESS_TOKEN = "quickAccessToken_TEST";

    //@ParameterizedTest
    //@MethodSource("legalFactGeneratorTestCases")
    void testLegalFactGeneration(LegalFactTestCase testCase) {
        @SuppressWarnings("unchecked")
        OngoingStubbing<Object> stubbing = (OngoingStubbing<Object>) testCase.getMockBehavior().apply(templatesClient);
        stubbing.thenReturn(testCase.getExpectedResult());

        var result = Assertions.assertDoesNotThrow(() -> testCase.execute(legalFactGeneratorTemplatesTest));
        Assertions.assertNotNull(result);
    }

    private static Stream<LegalFactTestCase> legalFactGeneratorTestCases() {
        return Stream.of(
                new LegalFactTestCase(
                        "generateNotificationReceivedLegalFact",
                        generator -> generator.generateNotificationReceivedLegalFact(notificationInt()),
                        client -> Mockito.when(client.notificationReceivedLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationReceivedLegalFact.class))),
                        byteArray
                ),
                new LegalFactTestCase(
                        "generateNotificationViewedLegalFact",
                        generator -> generator.generateNotificationViewedLegalFact(IUN, notificationRecipientInt(), delegateInfoInt(), Instant.now(), notificationInt()),
                        client -> Mockito.when(client.notificationViewedLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationViewedLegalFact.class))),
                        byteArray
                ),
                new LegalFactTestCase(
                        "generatePecDeliveryWorkflowLegalFact",
                        generator -> generator.generatePecDeliveryWorkflowLegalFact(
                                List.of(sendDigitalFeedbackDetailsInt()), notificationInt(), notificationRecipientInt(), EndWorkflowStatus.SUCCESS, Instant.now()
                        ),
                        client -> Mockito.when(client.pecDeliveryWorkflowLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(PecDeliveryWorkflowLegalFact.class))),
                        byteArray
                ),
                new LegalFactTestCase(
                        "generateAnalogDeliveryFailureWorkflowLegalFact",
                        generator -> generator.generateAnalogDeliveryFailureWorkflowLegalFact(notificationInt(), notificationRecipientInt(), EndWorkflowStatus.SUCCESS, Instant.now()),
                        client -> Mockito.when(client.analogDeliveryWorkflowFailureLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(AnalogDeliveryWorkflowFailureLegalFact.class))),
                        byteArray
                ),
                new LegalFactTestCase(
                        "generateNotificationCancelledLegalFact",
                        generator -> generator.generateNotificationCancelledLegalFact(notificationInt(), Instant.now()),
                        client -> Mockito.when(client.notificationCancelledLegalFact(Mockito.any(LanguageEnum.class), Mockito.any(NotificationCancelledLegalFact.class))),
                        byteArray
                ),
                new LegalFactTestCase(
                        "generateNotificationAARSubject",
                        generator -> generator.generateNotificationAARSubject(notificationInt()),
                        client -> Mockito.when(client.notificationAarForSubject(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForSubject.class))),
                        TEST_RETURN
                ),
                new LegalFactTestCase(
                        "generateNotificationAARBody",
                        generator -> generator.generateNotificationAARBody(notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN),
                        client -> Mockito.when(client.notificationAarForEmail(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForEmail.class))),
                        TEST_RETURN
                ),
                new LegalFactTestCase(
                        "generateNotificationAARPECBody",
                        generator -> generator.generateNotificationAARPECBody(notificationInt(), notificationRecipientInt(), QUICK_ACCESS_TOKEN),
                        client -> Mockito.when(client.notificationAarForPec(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForPec.class))),
                        TEST_RETURN
                ),
                new LegalFactTestCase(
                        "generateNotificationAARForSMS",
                        generator -> generator.generateNotificationAARForSMS(notificationInt()),
                        client -> Mockito.when(client.notificationAarForSms(Mockito.any(LanguageEnum.class), Mockito.any(NotificationAarForSms.class))),
                        TEST_RETURN
                )
        );
    }

    static class LegalFactTestCase {
        private final String testName;
        private final ThrowingFunction<LegalFactGenerator, Object> testFunction;
        private final Function<TemplatesClientImpl, OngoingStubbing<?>> mockBehavior;
        private final Object expectedResult;

        LegalFactTestCase(String testName, ThrowingFunction<LegalFactGenerator, Object> testFunction,
                          Function<TemplatesClientImpl, OngoingStubbing<?>> mockBehavior, Object expectedResult) {
            this.testName = testName;
            this.testFunction = testFunction;
            this.mockBehavior = mockBehavior;
            this.expectedResult = expectedResult;
        }

        Object execute(LegalFactGenerator generator) throws Throwable {
            return testFunction.apply(generator);
        }

        Function<TemplatesClientImpl, OngoingStubbing<?>> getMockBehavior() {
            return mockBehavior;
        }

        Object getExpectedResult() {
            return expectedResult;
        }

        String getTestName() {
            return testName;
        }
    }

    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T t) throws Throwable;
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