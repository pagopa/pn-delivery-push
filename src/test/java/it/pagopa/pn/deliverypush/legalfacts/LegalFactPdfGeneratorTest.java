package it.pagopa.pn.deliverypush.legalfacts;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import freemarker.template.Configuration;
import freemarker.template.Version;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnReadFileException;
import it.pagopa.pn.deliverypush.utils.HtmlSanitizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class LegalFactPdfGeneratorTest {
	private static final String TEST_DIR_NAME = "target" + File.separator + "generated-test-PDF";
	private static final Path TEST_DIR_PATH = Paths.get(TEST_DIR_NAME);

	@Mock
	private MVPParameterConsumer mvpParameterConsumer;

	private LegalFactGenerator pdfUtils;
	
	@BeforeEach
	public void setup() throws IOException {
		Configuration freemarker = new Configuration(new Version(2,3,0)); //Version is a final class
		HtmlSanitizer htmlSanitizer = new HtmlSanitizer(buildObjectMapper(), HtmlSanitizer.SanitizeMode.ESCAPING);
		DocumentComposition documentComposition = new DocumentComposition(freemarker, htmlSanitizer);

		CustomInstantWriter instantWriter = new CustomInstantWriter();
		PhysicalAddressWriter physicalAddressWriter = new PhysicalAddressWriter();
		PnDeliveryPushConfigs pnDeliveryPushConfigs = new PnDeliveryPushConfigs();
		InstantNowSupplier instantNowSupplier = new InstantNowSupplier();
		pnDeliveryPushConfigs.setWebapp(new PnDeliveryPushConfigs.Webapp());
		pnDeliveryPushConfigs.getWebapp().setFaqUrlTemplateSuffix("faq");
		pnDeliveryPushConfigs.getWebapp().setLandingUrl("https://www.dev.pn.pagopa.it");
		pnDeliveryPushConfigs.getWebapp().setFaqSendHash("send-cosa-e");
		pnDeliveryPushConfigs.getWebapp().setFaqCompletionMomentHash("perfezionamento-quando");
		pnDeliveryPushConfigs.getWebapp().setDirectAccessUrlTemplatePhysical("https://notifichedigitali.it");
		pnDeliveryPushConfigs.getWebapp().setDirectAccessUrlTemplateLegal("https://notifichedigitali.legal.it");
		pnDeliveryPushConfigs.getWebapp().setQuickAccessUrlAarDetailSuffix("aar");
		pnDeliveryPushConfigs.setPaperChannel(new PnDeliveryPushConfigs.PaperChannel());
		pnDeliveryPushConfigs.getPaperChannel().setSenderAddress( new PnDeliveryPushConfigs.SenderAddress());
		pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setFullname("PagoPA S.p.A.");
		pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setAddress("Via Sardegna n. 38");
		pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setZipcode("00187");
		pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setCity("Roma");
		pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setPr("Roma");
		pnDeliveryPushConfigs.getPaperChannel().getSenderAddress().setCountry("Italia");

		pdfUtils = new LegalFactGenerator(documentComposition, instantWriter, physicalAddressWriter, pnDeliveryPushConfigs, instantNowSupplier, mvpParameterConsumer);

		//create target test folder, if not exists
		if (Files.notExists(TEST_DIR_PATH)) { 
			Files.createDirectory(TEST_DIR_PATH);
		}
	}

	@Test 
	void generateNotificationReceivedLegalFactTest() throws IOException {	
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ReceivedLegalFact.pdf");
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationReceivedLegalFact(buildNotification())));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}

	@Test 
	void generateNotificationViewedLegalFactTest() throws IOException {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ViewedLegalFact.pdf");
		String iun = "iun1234Test_Viewed";
		NotificationRecipientInt recipient = buildRecipients().get(0);
		Instant notificationViewedDate = Instant.now().minus(Duration.ofMinutes(3));
		
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationViewedLegalFact(iun, recipient, null, notificationViewedDate)));
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
				.build();
		Instant notificationViewedDate = Instant.now().minus(Duration.ofMinutes(3));

		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationViewedLegalFact(iun, recipient, delegateInfo, notificationViewedDate)));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}

	@Test
	void generateAnalogDeliveryFailureWorkflowLegalFact() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_AnalogDeliveryFailureWorkflowLegalFact.pdf");
		List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList( ResponseStatusInt.OK);
		NotificationInt notification = buildNotification();
		NotificationRecipientInt recipient = buildRecipients().get(0);
		EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
		Instant sentDate = Instant.now().minus(Duration.ofDays(1));

		Assertions.assertDoesNotThrow(() -> {
			return Files.write(filePath, pdfUtils.generateAnalogDeliveryFailureWorkflowLegalFact( notification, recipient, endWorkflowStatus, sentDate));
		});
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generatePecDeliveryWorkflowLegalFactTest_OK() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_OK.pdf");
		List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList( ResponseStatusInt.OK);
		NotificationInt notification = buildNotification();
		NotificationRecipientInt recipient = buildRecipients().get(0);
		EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
		Instant sentDate = Instant.now().minus(Duration.ofDays(1));

		Assertions.assertDoesNotThrow(() -> {
			return Files.write(filePath, pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification, recipient, endWorkflowStatus, sentDate));
		});
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}

	@Test
	void generatePecDeliveryWorkflowLegalFactTestWithSpecialChar_OK() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_OK.pdf");
		List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList( ResponseStatusInt.OK);
		NotificationInt notification = buildNotification();
		NotificationRecipientInt recipient = buildRecipientsWithSpecialChar().get(0);
		EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
		Instant sentDate = Instant.now().minus(Duration.ofDays(1));

		Assertions.assertDoesNotThrow(() -> {
			return Files.write(filePath, pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification, recipient, endWorkflowStatus, sentDate));
		});
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generatePecDeliveryWorkflowLegalFactTest_KO() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_KO.pdf");
		List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList(ResponseStatusInt.KO);
		NotificationInt notification = buildNotification();
		NotificationRecipientInt recipient = buildRecipients().get(0);
		EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;

		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification, recipient, endWorkflowStatus, Instant.now())));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generategenerateFileComplianceTest() throws IOException {	
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_FileCompliance.pdf");
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateFileCompliance("PDF file name whitout extension", "test signature", Instant.now())));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}

	@Test
	@ExtendWith(SpringExtension.class)
	void generateNotificationAARTest() throws IOException {
		Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(false);

		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR.pdf");
		NotificationInt notificationInt = buildNotification();
		String quickAccessToken = "test";
		NotificationRecipientInt recipient = notificationInt.getRecipients().get(0).toBuilder().recipientType( RecipientTypeInt.PF ).build();
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test
	@ExtendWith(SpringExtension.class)
	void generateNotificationAARMVPTest() throws IOException {
		Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(true);
		
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAARMVP.pdf");
		NotificationInt notificationInt = buildNotification();
		String quickAccessToken = "test";
		NotificationRecipientInt recipient = notificationInt.getRecipients().get(0);
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}


	@Test
	@ExtendWith(SpringExtension.class)
    void generateNotificationAARPGTest() throws IOException {
		Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(false);

		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR.pdf");

        NotificationInt notificationInt = buildNotification();
		String quickAccessToken = "test";
		NotificationRecipientInt recipient = notificationInt.getRecipients().get(0).toBuilder().recipientType( RecipientTypeInt.PF ).build();
        Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationAAR(notificationInt, recipient, quickAccessToken)));
        System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
    }
	
	@Test
	void generateNotificationAAREmailTest() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_EMAIL.html");
		NotificationInt notificationInt = buildNotification();
		NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);
		String quickAccesstoken = "quickaccesstoken123";

		Assertions.assertDoesNotThrow(() -> {
					String element = pdfUtils.generateNotificationAARBody(notificationInt, notificationRecipientInt, quickAccesstoken);
					PrintWriter out = new PrintWriter(filePath.toString());
					out.println(element);
					out.close();
					System.out.println("element "+element);
				}
		);

		System.out.print("*** AAR PEC BODY successfully created");
	}


	@Test
	void generateNotificationAAREmailTest_Legal() throws IOException {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_EMAIL.html");
		NotificationInt notificationInt = buildNotification();
		NotificationRecipientInt recipient = buildRecipientsLegalWithSpecialChar().get(0);
		String quickAccesstoken = "quickaccesstoken123";

		Assertions.assertDoesNotThrow(() -> {
					String element = pdfUtils.generateNotificationAARBody(notificationInt, recipient, quickAccesstoken);
					PrintWriter out = new PrintWriter(filePath.toString());
					out.println(element);

					System.out.println("element "+element);
				}
		);

		System.out.print("*** AAR PEC BODY successfully created");
	}

	@Test
	void generateNotificationAARPECTest() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_PEC.html");

		NotificationInt notificationInt = buildNotification();
		NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);
		String quickAccessToken = "test";

		Assertions.assertDoesNotThrow(() -> {
					String element = pdfUtils.generateNotificationAARPECBody(notificationInt, notificationRecipientInt, quickAccessToken);
					PrintWriter out = new PrintWriter(filePath.toString());
					out.println(element);
					out.close();
		
					System.out.println("element "+element);
				}
		);
	}

	@Test
	void generateNotificationAAREMAILTest() {
		NotificationInt notificationInt = buildNotification();
		NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);
		String quickAccesstoken = "quickaccesstoken123";

		Assertions.assertDoesNotThrow(() -> pdfUtils.generateNotificationAARBody(notificationInt, notificationRecipientInt, quickAccesstoken));

		System.out.print("*** AAR EMAIL BODY successfully created");
	}

	@Test
	void generateNotificationAARForSmsTest() {

		NotificationInt notificationInt = buildNotification();

		Assertions.assertDoesNotThrow(() ->{
					String element = pdfUtils.generateNotificationAARForSMS(notificationInt);
					System.out.println("Notification AAR for SMS is "+element);
				}
		);

		System.out.print("*** AAR EMAIL BODY successfully created");
	}
	
	@Test
	void generateNotificationAARSubjectTest() throws IOException {
		NotificationInt notificationInt = buildNotification();

		Assertions.assertDoesNotThrow(() -> {
			String element = pdfUtils.generateNotificationAARSubject(notificationInt);
			System.out.println("Notification AarSubject is "+element);
		});

		System.out.print("*** AAR subject successfully created");
	}

	private List<SendDigitalFeedbackDetailsInt> buildFeedbackFromECList(ResponseStatusInt status) {
		SendDigitalFeedbackDetailsInt sdf = SendDigitalFeedbackDetailsInt.builder()
				.recIndex( 0 )
				.digitalAddress(LegalDigitalAddressInt.builder()
						.type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
						.address("prova@test.com")
						.build())
				.digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
				.responseStatus(ResponseStatusInt.KO)
				.notificationDate(Instant.now().minus(10, ChronoUnit.MINUTES))
				.build();

		SendDigitalFeedbackDetailsInt sdf2 = SendDigitalFeedbackDetailsInt.builder()
				.recIndex( 0 )
				.digitalAddress(LegalDigitalAddressInt.builder()
						.type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
						.address("pçroà2@test.com")
						.build())
				.digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
				.responseStatus(status)
				.notificationDate(Instant.now().minus(5, ChronoUnit.MINUTES))
				.build();

		SendDigitalFeedbackDetailsInt sdf3 = SendDigitalFeedbackDetailsInt.builder()
				.recIndex( 0 )
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
				.subject("notification test subject")
				.documents(Arrays.asList(
						NotificationDocumentInt.builder()
							.ref( NotificationDocumentInt.Ref.builder()
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
						"IT"
				))
				.build();
		
		return Collections.singletonList( rec1 );
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
						"IT"
				))
				.build();

		return Collections.singletonList( rec1 );
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
						"IT"
				))
				.build();

		return Collections.singletonList( rec1 );
	}

	private NotificationSenderInt createSender() {
		return NotificationSenderInt.builder()
				.paId("TEST_PA_ID")
				.paTaxId("TEST_TAX_ID")
				.paDenomination("TEST_PA_DENOMINATION")
				.build();
	}

	private ObjectMapper buildObjectMapper() {
		ObjectMapper objectMapper = ((JsonMapper.Builder)((JsonMapper.Builder)JsonMapper.builder().configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)).build();
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}
}
