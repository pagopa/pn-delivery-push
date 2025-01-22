package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.action.it.CommonTestConfiguration;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SpringBootTest
@TestPropertySource(properties = {"pn.delivery-push.enable-templates-engine=false"})
@Disabled("Test fail sometimes")
class LegalFactGeneratorDocCompositionTest extends CommonTestConfiguration {

    public static final String IUN = "IUN_TEST";
    public static final String QUICK_ACCESS_TOKEN = "quickAccessToken";

    @Autowired
    LegalFactGenerator legalFactGeneratorDocComposition;

    @ParameterizedTest
    @MethodSource("legalFactGeneratorDocCompositionTestCases")
    void testLegalFactGeneration(LegalFactTestCase testCase) {
        var result = Assertions.assertDoesNotThrow(() -> testCase.execute(legalFactGeneratorDocComposition));
        Assertions.assertNotNull(result);

        if (testCase.getExpectedSubstring() != null) {
            Assertions.assertTrue(result.toString().contains(testCase.getExpectedSubstring()));
        }

        if (testCase.getExpectedExactResult() != null) {
            Assertions.assertEquals(testCase.getExpectedExactResult(), result);
        }
    }

    private static Stream<LegalFactTestCase> legalFactGeneratorDocCompositionTestCases() {
        return Stream.of(
                new LegalFactTestCase(
                        "generateNotificationReceivedLegalFact",
                        legalFactGenerator -> legalFactGenerator.generateNotificationReceivedLegalFact(notificationInt())
                ),
                new LegalFactTestCase(
                        "generateNotificationCancelledLegalFact",
                        legalFactGenerator -> legalFactGenerator.generateNotificationCancelledLegalFact(notificationInt(), Instant.now())
                ),
                new LegalFactTestCase(
                        "generateNotificationViewedLegalFact",
                        legalFactGenerator -> legalFactGenerator.generateNotificationViewedLegalFact(
                                IUN, notificationRecipientInt(), delegateInfoInt(), Instant.now(), notificationInt()
                        )
                ),
                new LegalFactTestCase(
                        "generatePecDeliveryWorkflowLegalFact",
                        legalFactGenerator -> legalFactGenerator.generatePecDeliveryWorkflowLegalFact(
                                feedbackFromExtChannelList(), notificationInt(), notificationRecipientInt(),
                                EndWorkflowStatus.SUCCESS, Instant.now()
                        )
                ),
                new LegalFactTestCase(
                        "generateAnalogDeliveryFailureWorkflowLegalFact",
                        legalFactGenerator -> legalFactGenerator.generateAnalogDeliveryFailureWorkflowLegalFact(
                                notificationInt(), notificationRecipientInt(), EndWorkflowStatus.SUCCESS, Instant.now()
                        )
                ),
                new LegalFactTestCase(
                        "generateNotificationAAR",
                        legalFactGenerator -> legalFactGenerator.generateNotificationAAR(notificationInt(),
                                notificationRecipientInt(), QUICK_ACCESS_TOKEN
                        )
                ),
                new LegalFactTestCase(
                        "generateNotificationAARBody",
                        legalFactGenerator -> legalFactGenerator.generateNotificationAARBody(notificationInt(),
                                notificationRecipientInt(), QUICK_ACCESS_TOKEN
                        ),
                        "hai inserito il tuo indirizzo e-mail tra i recapiti di cortesia di SEND"
                ),
                new LegalFactTestCase(
                        "generateNotificationAARPECBody",
                        legalFactGenerator -> legalFactGenerator.generateNotificationAARPECBody(notificationInt(),
                                notificationRecipientInt(), QUICK_ACCESS_TOKEN
                        ),
                        "ricezione in qualità di persona fisica con Codice Fiscale recipient_taxId"
                ),
                new LegalFactTestCase(
                        "generateNotificationAARSubject",
                        legalFactGenerator -> legalFactGenerator.generateNotificationAARSubject(notificationInt()),
                        null,
                        "SEND - Nuova notifica da paDenomination_TEST_TEST - iun_TEST_TEST"
                ),
                new LegalFactTestCase(
                        "generateNotificationAARForSMS",
                        legalFactGenerator -> legalFactGenerator.generateNotificationAARForSMS(notificationInt()),
                        "Hai ricevuto una notifica da paDenomination_TEST_TEST con Codice IUN iun_TEST_TEST"
                )
        );
    }

    private static List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList() {
        return List.of(SendDigitalFeedbackDetailsInt.builder()
                .recIndex(12)
                .notificationDate(Instant.now())
                .responseStatus(ResponseStatusInt.OK)
                .digitalAddress(legalDigitalAddressInt())
                .deliveryDetailCode("deliveryDetailCode")
                .build());
    }

    private static LegalDigitalAddressInt legalDigitalAddressInt() {
        return LegalDigitalAddressInt.builder()
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .address("LegalDigitalAddress_TEST_TEST")
                .build();
    }

    private static DelegateInfoInt delegateInfoInt() {
        return DelegateInfoInt.builder()
                .denomination("delegate_denomination_TEST")
                .taxId("delegate_taxId_TEST")
                .build();
    }

    private static NotificationRecipientInt notificationRecipientInt() {
        return NotificationRecipientInt.builder()
                .taxId("recipient_taxId_TEST_TEST")
                .physicalAddress(physicalAddressInt())
                .recipientType(RecipientTypeInt.PF)
                .denomination("recipient_denomination_TEST_TEST")
                .build();
    }

    private static PhysicalAddressInt physicalAddressInt() {
        return PhysicalAddressInt.builder()
                .zip("00000")
                .build();
    }

    private static NotificationInt notificationInt() {
        return NotificationInt.builder()
                .subject("subject_TEST_TEST")
                .additionalLanguages(new ArrayList<>())
                .iun("iun_TEST_TEST")
                .sentAt(Instant.now())
                .sender(notificationSenderInt())
                .documents(new ArrayList<>())
                .recipients(new ArrayList<>())
                .build();
    }

    private static NotificationSenderInt notificationSenderInt() {
        return NotificationSenderInt.builder()
                .paTaxId("paTaxId_TEST_TEST")
                .paDenomination("paDenomination_TEST_TEST")
                .build();
    }

    static class LegalFactTestCase {
        private final String testName;
        private final ThrowingFunction<LegalFactGenerator, Object> testFunction;
        private final String expectedSubstring;
        private final String expectedExactResult;

        LegalFactTestCase(String testName, ThrowingFunction<LegalFactGenerator, Object> testFunction) {
            this(testName, testFunction, null, null);
        }

        LegalFactTestCase(String testName, ThrowingFunction<LegalFactGenerator, Object> testFunction, String expectedSubstring) {
            this(testName, testFunction, expectedSubstring, null);
        }

        LegalFactTestCase(String testName, ThrowingFunction<LegalFactGenerator, Object> testFunction, String expectedSubstring, String expectedExactResult) {
            this.testName = testName;
            this.testFunction = testFunction;
            this.expectedSubstring = expectedSubstring;
            this.expectedExactResult = expectedExactResult;
        }

        String getTestName() {
            return testName;
        }

        Object execute(LegalFactGenerator legalFactGenerator) throws Throwable {
            return testFunction.apply(legalFactGenerator);
        }

        String getExpectedSubstring() {
            return expectedSubstring;
        }

        String getExpectedExactResult() {
            return expectedExactResult;
        }
    }

        @FunctionalInterface
        interface ThrowingFunction<T, R> {
            R apply(T t) throws Throwable;
        }
}