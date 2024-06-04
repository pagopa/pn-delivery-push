package it.pagopa.pn.deliverypush.legalfacts;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import freemarker.template.Configuration;
import freemarker.template.Version;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.utils.HtmlSanitizer;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

class LegalFactPdfGeneratorTest {
        private static final String TEST_DIR_NAME = "target" + File.separator + "generated-test-PDF";
        private static final Path TEST_DIR_PATH = Paths.get(TEST_DIR_NAME);

        @Mock
        private PnSendModeUtils pnSendModeUtils;

        private LegalFactGenerator pdfUtils;

        @BeforeEach
        public void setup() throws IOException {
                Configuration freemarker = new Configuration(new Version(2, 3, 0)); // Version is a final class
                HtmlSanitizer htmlSanitizer = new HtmlSanitizer(buildObjectMapper(),
                                HtmlSanitizer.SanitizeMode.ESCAPING);
                DocumentComposition documentComposition = new DocumentComposition(freemarker, htmlSanitizer);

                CustomInstantWriter instantWriter = new CustomInstantWriter();
                PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();
                PnDeliveryPushConfigs pnDeliveryPushConfigs = new PnDeliveryPushConfigs();
                InstantNowSupplier instantNowSupplier = new InstantNowSupplier();
                pnDeliveryPushConfigs.setWebapp(new PnDeliveryPushConfigs.Webapp());
                pnDeliveryPushConfigs.getWebapp().setFaqUrlTemplateSuffix("faq");
                pnDeliveryPushConfigs.getWebapp().setLandingUrl("https://notifichedigitali.pagopa.it/");
                pnDeliveryPushConfigs.getWebapp().setFaqSendHash("send-cosa-e");
                pnDeliveryPushConfigs.getWebapp().setFaqCompletionMomentHash("perfezionamento");
                pnDeliveryPushConfigs.getWebapp()
                                .setDirectAccessUrlTemplatePhysical("https://cittadini.notifichedigitali.it/");
                pnDeliveryPushConfigs.getWebapp()
                                .setDirectAccessUrlTemplateLegal("https://imprese.notifichedigitali.it/");
                pnDeliveryPushConfigs.getWebapp().setQuickAccessUrlAarDetailSuffix("?aar");
                Map<String, String> additional = new HashMap<>();
                additional.put("raddoperatorcaf", "true");
                additional.put("raddoperatormooney", "false");
                additional.put("raddoperatorsailpost", "false");
                pnDeliveryPushConfigs.getWebapp().setAdditional(additional);
                pnDeliveryPushConfigs.getWebapp().setLegalFactDisclaimer("prova disclaimer");
                pnDeliveryPushConfigs.setPaperChannel(new PnDeliveryPushConfigs.PaperChannel());
                pnDeliveryPushConfigs.getPaperChannel().setSenderAddress(new PnDeliveryPushConfigs.SenderAddress());
                pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setFullname("PagoPA S.p.A.");
                pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setAddress("Via Sardegna n. 38");
                pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setZipcode("00187");
                pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setCity("Roma");
                pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setPr("Roma");
                pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setCountry("Italia");
                pnDeliveryPushConfigs.setErrorCorrectionLevelQrCode(ErrorCorrectionLevel.H);

                pdfUtils = new LegalFactGenerator(documentComposition, instantWriter, physicalAddressWriter,
                                pnDeliveryPushConfigs, instantNowSupplier, pnSendModeUtils);

                // create target test folder, if not exists
                if (Files.notExists(TEST_DIR_PATH)) {
                        Files.createDirectory(TEST_DIR_PATH);
                }
        }

        @Test
        void generateNotificationReceivedLegalFactTest() throws IOException {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ReceivedLegalFact.pdf");
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationReceivedLegalFact(buildNotification())));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generateNotificationReceivedLegalFactTestWithSinglePaymentPagoPA() throws IOException {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ReceivedLegalFact.pdf");
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationReceivedLegalFact(buildNotificationWithSinglePayment())));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generateNotificationReceivedLegalFactTestWithMultipaymentPagoPaAndF24() throws IOException {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ReceivedLegalFact.pdf");
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationReceivedLegalFact(buildNotificationWithMultiPayment())));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generateNotificationViewedLegalFactTest() throws IOException {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ViewedLegalFact.pdf");
                String iun = "iun1234Test_Viewed";
                NotificationRecipientInt recipient = buildRecipients().get(0);
                Instant notificationViewedDate = Instant.now().minus(Duration.ofMinutes(3));

                Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils
                                .generateNotificationViewedLegalFact(iun, recipient, null, notificationViewedDate)));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generateNotificationDelegateViewedLegalFactTest() throws IOException {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_DelegateViewedLegalFact.pdf");
                String iun = "iun1234Test_Viewed";
                NotificationRecipientInt recipient = buildRecipients().get(0);
                DelegateInfoInt delegateInfo = DelegateInfoInt.builder()
                                .denomination("Mario Rossi")
                                .taxId("RSSMRA80A01H501U")
                                .delegateType(RecipientTypeInt.PF)
                                .build();
                Instant notificationViewedDate = Instant.now().minus(Duration.ofMinutes(3));

                Assertions.assertDoesNotThrow(
                                () -> Files.write(filePath, pdfUtils.generateNotificationViewedLegalFact(iun, recipient,
                                                delegateInfo, notificationViewedDate)));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generateAnalogDeliveryFailureWorkflowLegalFact() {
                Path filePath = Paths.get(
                                TEST_DIR_NAME + File.separator + "test_AnalogDeliveryFailureWorkflowLegalFact.pdf");
                List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList(
                                ResponseStatusInt.OK);
                NotificationInt notification = buildNotification();
                NotificationRecipientInt recipient = buildRecipients().get(0);
                EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
                Instant sentDate = Instant.now().minus(Duration.ofDays(1));

                Assertions.assertDoesNotThrow(() -> {
                        return Files.write(filePath, pdfUtils.generateAnalogDeliveryFailureWorkflowLegalFact(
                                        notification, recipient, endWorkflowStatus, sentDate));
                });
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generatePecDeliveryWorkflowLegalFactTest_OK() {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_OK.pdf");
                List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList(
                                ResponseStatusInt.OK);
                NotificationInt notification = buildNotification();
                NotificationRecipientInt recipient = buildRecipients().get(0);
                EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
                Instant sentDate = Instant.now().minus(Duration.ofDays(1));

                Assertions.assertDoesNotThrow(() -> {
                        return Files.write(filePath,
                                        pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList,
                                                        notification, recipient, endWorkflowStatus, sentDate));
                });
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generatePecDeliveryWorkflowLegalFactTestWithSpecialChar_OK() {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_OK.pdf");
                List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList(
                                ResponseStatusInt.OK);
                NotificationInt notification = buildNotification();
                NotificationRecipientInt recipient = buildRecipientsWithSpecialChar().get(0);
                EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
                Instant sentDate = Instant.now().minus(Duration.ofDays(1));

                Assertions.assertDoesNotThrow(() -> {
                        return Files.write(filePath,
                                        pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList,
                                                        notification, recipient, endWorkflowStatus, sentDate));
                });
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generatePecDeliveryWorkflowLegalFactTest_KO() {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_KO.pdf");
                List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList(
                                ResponseStatusInt.KO);
                NotificationInt notification = buildNotification();
                NotificationRecipientInt recipient = buildRecipients().get(0);
                EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification,
                                                recipient, endWorkflowStatus, Instant.now())));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        @ExtendWith(SpringExtension.class)
        void generateNotificationAARTest() {
                Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                                .build());

                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR.pdf");
                NotificationInt notificationInt = buildNotification();
                String quickAccessToken = "test";
                NotificationRecipientInt recipient = notificationInt.getRecipients().get(0).toBuilder()
                                .recipientType(RecipientTypeInt.PF).build();
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
                System.out.print("*** AAR pdf successfully created at: " + filePath);
        }

        @Test
        @ExtendWith(SpringExtension.class)
        void generateNotificationAarError() {
                Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(null);
                
                NotificationInt notificationInt = buildNotification();
                String quickAccessToken = "test";
                NotificationRecipientInt recipient = notificationInt.getRecipients().get(0).toBuilder().recipientType(RecipientTypeInt.PF).build();
                Assertions.assertThrows(PnInternalException.class, () -> pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken));
        }

        @Test
        @ExtendWith(SpringExtension.class)
        void generateNotificationAAR_RADDPFTest() {
                Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD)
                                .build());

                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_RADD_PF.pdf");
                NotificationSenderInt notificationSenderInt = NotificationSenderInt.builder()
                                .paId("TEST_PA_ID")
                                .paTaxId("TEST_TAX_ID")
                                .paDenomination("Ente per la Gestione de Parco Regionale di Montevecchia e della Valle del Curone")
                                .build();

                NotificationInt notificationInt = NotificationInt.builder()
                                .sender(notificationSenderInt)
                                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                                .iun("Example_IUN_1234_Test")
                                .subject("Titolo: RPE2E0121020003 E2E_01 WEB run003 del 09/11/2023 14: 50Titolo: RPE2E0121020003 E2E_01 WEB run003 del 09/11/2023 14: 50Titolo:III")
                                .build();
                String quickAccessToken = "test";
                NotificationRecipientInt recipient = NotificationRecipientInt.builder()
                                .recipientType(RecipientTypeInt.PF)
                                .denomination("Antonio Griffo Focas Flavio Angelo Ducas Comeno Porfirogenito Gagliardi De Curti")
                                .taxId("RSSMRA80A01H501U")
                                .build();
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        @ExtendWith(SpringExtension.class)
        void generateNotificationAAR_RADD_ALT_Test() {
                Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                        .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD_ALT)
                        .build());

                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_RADDalt.pdf");
                NotificationSenderInt notificationSenderInt = NotificationSenderInt.builder()
                        .paId("TEST_PA_ID")
                        .paTaxId("TEST_TAX_ID")
                        .paDenomination("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque porttitore")
                        .build();

                NotificationInt notificationInt = NotificationInt.builder()
                        .sender(notificationSenderInt)
                        .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                        .iun("Example_IUN_1234_Test")
                        .subject("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas et libero velit. Cras dignissim consequat ornare. Etiam sed justo sit. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas et libero velit. Cras dignissim consequat ornare. Etiam sed justo sit.")
                        .build();
                String quickAccessToken = "test";
                NotificationRecipientInt recipient = NotificationRecipientInt.builder()
                        .recipientType(RecipientTypeInt.PF)
                        .denomination("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque porttitore")
                        .taxId("LRMPSM80A01H501U")
                        .build();
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                        pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        @ExtendWith(SpringExtension.class)
        void generateNotificationAAR_RADDPGTest() throws IOException {
                Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD)
                                .build());

                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_RADD_PG.pdf");
                NotificationSenderInt notificationSenderInt = NotificationSenderInt.builder()
                                .paId("TEST_PA_ID")
                                .paTaxId("TEST_TAX_ID")
                                .paDenomination("Ente per la Gestione de Parco Regionale di Montevecchia e della Valle del Curone")
                                .build();

                NotificationInt notificationInt = NotificationInt.builder()
                                .sender(notificationSenderInt)
                                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                                .iun("Example_IUN_1234_Test")
                                .subject("Titolo: RPE2E0121020003 E2E_01 WEB run003 del 09/11/2023 14: 50Titolo: RPE2E0121020003 E2E_01 WEB run003 del 09/11/2023 14: 50Titolo:III")
                                .build();
                String quickAccessToken = "test";
                NotificationRecipientInt recipient = NotificationRecipientInt.builder()
                                .recipientType(RecipientTypeInt.PG)
                                .denomination("Antonio Griffo Focas Flavio Angelo Ducas Comeno Porfirogenito Gagliardi De Curti")
                                .taxId("RSSMRA80A01H501U")
                                .build();
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        @ExtendWith(SpringExtension.class)
        void generateNotificationAAR_RADD_NumericPGTest() throws IOException {
                Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                        .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION_RADD)
                        .build());

                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_RADD_numericPG.pdf");
                NotificationSenderInt notificationSenderInt = NotificationSenderInt.builder()
                                .paId("TEST_PA_ID")
                                .paTaxId("TEST_TAX_ID")
                                .paDenomination("Ente per la Gestione de Parco Regionale di Montevecchia e della Valle del Curone")
                                .build();

                NotificationInt notificationInt = NotificationInt.builder()
                                .sender(notificationSenderInt)
                                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                                .iun("Example_IUN_1234_Test")
                                .subject("Titolo: RPE2E0121020003 E2E_01 WEB run003 del 09/11/2023 14: 50Titolo: RPE2E0121020003 E2E_01 WEB run003 del 09/11/2023 14: 50Titolo:III")
                                .build();
                String quickAccessToken = "test";
                NotificationRecipientInt recipient = NotificationRecipientInt.builder()
                                .recipientType(RecipientTypeInt.PG)
                                .denomination("Antonio Griffo Focas Flavio Angelo Ducas Comeno Porfirogenito Gagliardi De Curti")
                                .taxId("15376371009")
                                .build();
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        @ExtendWith(SpringExtension.class)
        void generateNotificationAARPGTest() {
                Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                                .build());
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_PG.pdf");

                NotificationInt notificationInt = buildNotification();
                String quickAccessToken = "test";
                NotificationRecipientInt recipient = notificationInt.getRecipients().get(0).toBuilder()
                                .recipientType(RecipientTypeInt.PG).build();
                Assertions.assertDoesNotThrow(() -> Files.write(filePath,
                                pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
                System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
        }

        @Test
        void generateNotificationAAREmailTest() {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_EMAIL.html");
                NotificationInt notificationInt = buildNotification();
                NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);
                String quickAccesstoken = "quickaccesstoken123";

                Assertions.assertDoesNotThrow(() -> {
                        String element = pdfUtils.generateNotificationAARBody(notificationInt, notificationRecipientInt,
                                        quickAccesstoken);
                        PrintWriter out = new PrintWriter(filePath.toString());
                        out.println(element);
                        out.close();
                        System.out.println("element " + element);
                });

                System.out.print("*** AAR EMAIL BODY successfully created");
        }

        @Test
        void generateNotificationAAREmailTest_Legal() throws IOException {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_EMAIL.html");
                NotificationInt notificationInt = buildNotification();
                NotificationRecipientInt recipient = buildRecipientsLegalWithSpecialChar().get(0);
                String quickAccesstoken = "quickaccesstoken123";

                Assertions.assertDoesNotThrow(() -> {
                        String element = pdfUtils.generateNotificationAARBody(notificationInt, recipient,
                                        quickAccesstoken);
                        PrintWriter out = new PrintWriter(filePath.toString());
                        out.println(element);

                        System.out.println("element " + element);
                });

                System.out.print("*** AAR EMAIL BODY successfully created");
        }

        @Test
        void generateNotificationAARPECTest() {
                Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_PEC.html");

                NotificationInt notificationInt = buildNotification();
                NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);
                String quickAccessToken = "test";

                Assertions.assertDoesNotThrow(() -> {
                        String element = pdfUtils.generateNotificationAARPECBody(notificationInt,
                                        notificationRecipientInt, quickAccessToken);
                        PrintWriter out = new PrintWriter(filePath.toString());
                        out.println(element);
                        out.close();

                        System.out.println("element " + element);
                });
        }

        @Test
        void generateNotificationAAREMAILTest() {
                NotificationInt notificationInt = buildNotification();
                NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);
                String quickAccesstoken = "quickaccesstoken123";

                Assertions.assertDoesNotThrow(() -> pdfUtils.generateNotificationAARBody(notificationInt,
                                notificationRecipientInt, quickAccesstoken));

                System.out.print("*** AAR EMAIL BODY successfully created");
        }

        @Test
        void generateNotificationAARForSmsTest() {

                NotificationInt notificationInt = buildNotification();

                Assertions.assertDoesNotThrow(() -> {
                        String element = pdfUtils.generateNotificationAARForSMS(notificationInt);
                        System.out.println("Notification AAR for SMS is " + element);
                });

                System.out.print("*** AAR SMS successfully created");
        }

        @Test
        void generateNotificationAARSubjectTest() throws IOException {
                NotificationInt notificationInt = buildNotification();

                Assertions.assertDoesNotThrow(() -> {
                        String element = pdfUtils.generateNotificationAARSubject(notificationInt);
                        System.out.println("Notification AarSubject is " + element);
                });

                System.out.print("*** AAR subject successfully created");
        }

        private List<SendDigitalFeedbackDetailsInt> buildFeedbackFromECList(ResponseStatusInt status) {
                SendDigitalFeedbackDetailsInt sdf = SendDigitalFeedbackDetailsInt.builder()
                                .recIndex(0)
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                                .address("prova@test.com")
                                                .build())
                                .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                                .responseStatus(ResponseStatusInt.KO)
                                .notificationDate(Instant.now().minus(10, ChronoUnit.MINUTES))
                                .build();

                SendDigitalFeedbackDetailsInt sdf2 = SendDigitalFeedbackDetailsInt.builder()
                                .recIndex(0)
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                                .address("pçroà2@test.com")
                                                .build())
                                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                                .responseStatus(status)
                                .notificationDate(Instant.now().minus(5, ChronoUnit.MINUTES))
                                .build();

                SendDigitalFeedbackDetailsInt sdf3 = SendDigitalFeedbackDetailsInt.builder()
                                .recIndex(0)
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                                .address("pçroà3@test.com")
                                                .build())
                                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                                .responseStatus(status)
                                .notificationDate(Instant.now().minus(5, ChronoUnit.MINUTES))
                                .build();

                List<SendDigitalFeedbackDetailsInt> result = new ArrayList<SendDigitalFeedbackDetailsInt>();
                result.add(sdf);
                result.add(sdf2);
                result.add(sdf3);
                return result;
        }

        private NotificationInt buildNotification() {
                return NotificationInt.builder()
                                .sender(createSender())
                                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                                .iun("Example_IUN_1234_Test")
                                .subject("notification Titolo di 134 caratteri massimi spazi compresi. Aid olotielit, sed eiusmod tempora incidunt ue et et dolore magna aliqua aliqua aliqua")
                                .documents(Arrays.asList(
                                                NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder()
                                                                                .key("doc00")
                                                                                .versionToken("v01_doc00")
                                                                                .build())
                                                                .digests(NotificationDocumentInt.Digests.builder()
                                                                                .sha256((Base64Utils.encodeToString(
                                                                                                "sha256_doc01".getBytes())))
                                                                                .build())
                                                                .build()))
                                .recipients(buildRecipients())
                                .build();
        }

        private NotificationInt buildNotificationWithSinglePayment() {
                return NotificationInt.builder()
                                .sender(createSender())
                                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                                .iun("Example_IUN_1234_Test")
                                .subject("notification test subject")
                                .documents(Arrays.asList(
                                                NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder()
                                                                                .key("doc00")
                                                                                .versionToken("v01_doc00")
                                                                                .build())
                                                                .digests(NotificationDocumentInt.Digests.builder()
                                                                                .sha256((Base64Utils.encodeToString(
                                                                                                "sha256_doc01".getBytes())))
                                                                                .build())
                                                                .build()))
                                .recipients(buildRecipientsSinglePayment())
                                .build();
        }

        private NotificationInt buildNotificationWithMultiPayment() {
                return NotificationInt.builder()
                                .sender(createSender())
                                .sentAt(Instant.now().minus(Duration.ofDays(1).minus(Duration.ofMinutes(10))))
                                .iun("Example_IUN_1234_Test")
                                .subject("notification test subject")
                                .documents(Arrays.asList(
                                                NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder()
                                                                                .key("doc00")
                                                                                .versionToken("v01_doc00")
                                                                                .build())
                                                                .digests(NotificationDocumentInt.Digests.builder()
                                                                                .sha256((Base64Utils.encodeToString(
                                                                                                "sha256_doc01".getBytes())))
                                                                                .build())
                                                                .build()))
                                .recipients(buildRecipientsMultiPayments())
                                .build();
        }

        private List<NotificationRecipientInt> buildRecipientsMultiPayments() {
                NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                                .taxId("CDCFSC11R99X001Z")
                                .recipientType(RecipientTypeInt.PF)
                                .denomination("Galileo Bruno")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                                .address("test@dominioPec.it")
                                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                                .build())
                                .payments(List.of(NotificationPaymentInfoInt.builder()
                                                .f24(F24Int.builder()
                                                                .applyCost(true)
                                                                .title("title")
                                                                .metadataAttachment(NotificationDocumentInt.builder()
                                                                                .digests(NotificationDocumentInt.Digests
                                                                                                .builder()
                                                                                                .sha256("YjBkNDU0YmI2YWQxY2Q2ZDE2ZDY2MmM0NmE0YmQzOTM5NDQ3MzVhNmFkMGEzNWI4NDg5Y2FkYzUxMDI3OGI3Yw==")
                                                                                                .build())
                                                                                .ref(NotificationDocumentInt.Ref
                                                                                                .builder().build())
                                                                                .contentType("contentType")
                                                                                .build())
                                                                .build())
                                                .pagoPA(PagoPaInt.builder()
                                                                .applyCost(true)
                                                                .attachment(NotificationDocumentInt.builder()
                                                                                .digests(NotificationDocumentInt.Digests
                                                                                                .builder()
                                                                                                .sha256("YjBkNDU0YmI2YWQxY2Q2ZDE2ZDY2MmM0NmE0YmQzOTM5NDQ3MzVhNmFkMGEzNWI4NDg5Y2FkYzUxMDI3OGI3Yw==")
                                                                                                .build())
                                                                                .contentType("contentType")
                                                                                .ref(NotificationDocumentInt.Ref
                                                                                                .builder().build())
                                                                                .build())
                                                                .creditorTaxId("taxId")
                                                                .noticeCode("noticeCode")
                                                                .build())
                                                .build()))
                                .physicalAddress(new PhysicalAddressInt(
                                                "Galileo Bruno",
                                                "Palazzo dell'Inquisizione",
                                                "corso Italia 666",
                                                "Piano Terra (piatta)",
                                                "00100",
                                                "Roma",
                                                null,
                                                "RM",
                                                "IT"))
                                .build();

                return Collections.singletonList(rec1);
        }

        private List<NotificationRecipientInt> buildRecipientsSinglePayment() {
                NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                                .taxId("CDCFSC11R99X001Z")
                                .recipientType(RecipientTypeInt.PF)
                                .denomination("Galileo Bruno")
                                .payments(Collections.singletonList(NotificationPaymentInfoInt.builder()
                                                .pagoPA(PagoPaInt.builder()
                                                                .creditorTaxId("taxId")
                                                                .noticeCode("noticeCode")
                                                                .attachment(NotificationDocumentInt.builder()
                                                                                .ref(NotificationDocumentInt.Ref
                                                                                                .builder().build())
                                                                                .contentType("contentType")
                                                                                .digests(NotificationDocumentInt.Digests
                                                                                                .builder()
                                                                                                .sha256("YjBkNDU0YmI2YWQxY2Q2ZDE2ZDY2MmM0NmE0YmQzOTM5NDQ3MzVhNmFkMGEzNWI4NDg5Y2FkYzUxMDI3OGI3Yw==")
                                                                                                .build())
                                                                                .build())
                                                                .build())
                                                .build()))
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
                                                "IT"))
                                .build();

                return Collections.singletonList(rec1);
        }

        private List<NotificationRecipientInt> buildRecipients() {
                NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                                .taxId("CDCFSC11R99X001Z")
                                .recipientType(RecipientTypeInt.PF)
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
                                                "IT"))
                                .build();

                return Collections.singletonList(rec1);
        }

        private List<NotificationRecipientInt> buildRecipientsWithSpecialChar() {
                NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                                .taxId("CDCFSC11R99X001Z")
                                .recipientType(RecipientTypeInt.PF)
                                .denomination("Galileo Brunè <h1>ciao</h1>")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                                .address("test@dominioàPec.it")
                                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                                .build())
                                .physicalAddress(new PhysicalAddressInt(
                                                "Galileo Bruno",
                                                "Palazzò dell'Inquisizionß",
                                                "corso Italia 666",
                                                "Pianô Terra (piatta)",
                                                "00100",
                                                "Roma",
                                                null,
                                                "RM",
                                                "IT"))
                                .build();

                return Collections.singletonList(rec1);
        }

        private List<NotificationRecipientInt> buildRecipientsLegalWithSpecialChar() {
                NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                                .taxId("CDCFSC11R99X001Z")
                                .recipientType(RecipientTypeInt.PG)
                                .denomination("Galileo Brunè <h1>ciao</h1>")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                                .address("test@dominioàPec.it")
                                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                                .build())
                                .physicalAddress(new PhysicalAddressInt(
                                                "Galileo Bruno",
                                                "Palazzò dell'Inquisizionß",
                                                "corso Italia 666",
                                                "Pianô Terra (piatta)",
                                                "00100",
                                                "Roma",
                                                null,
                                                "RM",
                                                "IT"))
                                .build();

                return Collections.singletonList(rec1);
        }

        private NotificationSenderInt createSender() {
                return NotificationSenderInt.builder()
                                .paId("TEST_PA_ID")
                                .paTaxId("TEST_TAX_ID")
                                .paDenomination("TEST_PA_DENOMINATION")
                                .build();
        }

        private ObjectMapper buildObjectMapper() {
                ObjectMapper objectMapper = ((JsonMapper.Builder) ((JsonMapper.Builder) JsonMapper.builder()
                                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false))
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)).build();
                objectMapper.registerModule(new JavaTimeModule());
                return objectMapper;
        }
}
