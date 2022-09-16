package it.pagopa.pn.deliverypush.legalfacts;

import freemarker.template.Configuration;
import freemarker.template.Version;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class LegalFactPdfGeneratorTest {
	private static final String TEST_DIR_NAME = "target" + File.separator + "generated-test-PDF";
	private static final Path TEST_DIR_PATH = Paths.get(TEST_DIR_NAME);

	private DocumentComposition documentComposition;
	private CustomInstantWriter instantWriter;
	private PhysicalAddressWriter physicalAddressWriter;
	private PnDeliveryPushConfigs pnDeliveryPushConfigs;
	private LegalFactGenerator pdfUtils;

	@BeforeEach
	public void setup() throws IOException {
		Configuration freemarker = new Configuration(new Version(2,3,0)); //Version is a final class
		documentComposition = new DocumentComposition(freemarker);
		
		instantWriter = new CustomInstantWriter();
		physicalAddressWriter = new PhysicalAddressWriter();
		pnDeliveryPushConfigs = new PnDeliveryPushConfigs();
		pnDeliveryPushConfigs.setWebapp(new PnDeliveryPushConfigs.Webapp());
		pnDeliveryPushConfigs.getWebapp().setFaqUrlTemplate("https://notifichedigitali.it/faq");
		pnDeliveryPushConfigs.getWebapp().setDirectAccessUrlTemplate("https://notifichedigitali.it/iun=%s");


		pdfUtils = new LegalFactGenerator(documentComposition, instantWriter, physicalAddressWriter, pnDeliveryPushConfigs);

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
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationViewedLegalFact(iun, recipient, Instant.now())));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generatePecDeliveryWorkflowLegalFactTest_OK() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_OK.pdf");
		List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList( ResponseStatusInt.OK);
		NotificationInt notification = buildNotification();
		NotificationRecipientInt recipient = buildRecipients().get(0);
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification, recipient)));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generatePecDeliveryWorkflowLegalFactTest_KO() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact_KO.pdf");
		List<SendDigitalFeedbackDetailsInt> feedbackFromExtChannelList = buildFeedbackFromECList(ResponseStatusInt.KO);
		NotificationInt notification = buildNotification();
		NotificationRecipientInt recipient = buildRecipients().get(0);
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification, recipient)));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generategenerateFileComplianceTest() throws IOException {	
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_FileCompliance.pdf");
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateFileCompliance("PDF file name whitout extension", "test signature", Instant.now())));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generateNotificationAARTest() throws IOException {	
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR.pdf");
		NotificationInt notificationInt = buildNotification();
		Assertions.assertDoesNotThrow(() -> Files.write(filePath, pdfUtils.generateNotificationAAR(notificationInt, notificationInt.getRecipients().get(0))));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}


	@Test
	void generateNotificationAARPECTest() throws IOException {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_EMAIL.html");
		NotificationInt notificationInt = buildNotification();
		NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);

		Assertions.assertDoesNotThrow(() -> {
					String element = pdfUtils.generateNotificationAARBody(notificationInt, notificationRecipientInt);
					PrintWriter out = new PrintWriter(filePath.toString());
					out.println(element);

					System.out.println("element "+element);
				}
		);

		System.out.print("*** AAR PEC BODY successfully created");
	}

	@Test
	void generateNotificationAARPECBodyTest() {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_NotificationAAR_PEC.html");

		NotificationInt notificationInt = buildNotification();
		NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);

		Assertions.assertDoesNotThrow(() -> {
					String element = pdfUtils.generateNotificationAARPECBody(notificationInt, notificationRecipientInt);
					PrintWriter out = new PrintWriter(filePath.toString());
					out.println(element);
		
					System.out.println("element "+element);
				}
		);
	}

	@Test
	void generateNotificationAAREMAILTest() {
		NotificationInt notificationInt = buildNotification();
		NotificationRecipientInt notificationRecipientInt = notificationInt.getRecipients().get(0);

		Assertions.assertDoesNotThrow(() -> pdfUtils.generateNotificationAARBody(notificationInt, notificationRecipientInt));

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
				.responseStatus(ResponseStatusInt.KO)
				.notificationDate(Instant.now())
				.build();

		SendDigitalFeedbackDetailsInt sdf2 = SendDigitalFeedbackDetailsInt.builder()
				.recIndex( 0 )
				.digitalAddress(LegalDigitalAddressInt.builder()
						.type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
						.address("prova2@test.com")
						.build())
				.responseStatus(ResponseStatusInt.OK)
				.notificationDate(Instant.now())
				.build();
		
		List<SendDigitalFeedbackDetailsInt> result = new ArrayList<SendDigitalFeedbackDetailsInt>();
		result.add(sdf);
		result.add(sdf2);
		return result;
	}

	private NotificationInt buildNotification() {
		return NotificationInt.builder()
				.sender(createSender())
				.sentAt(Instant.now())
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
				.denomination("Galileo Bruno")
				.digitalDomicile(LegalDigitalAddressInt.builder()
						.address("test@dominioPec.it")
						.type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
						.build())
				.physicalAddress(new PhysicalAddressInt(
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

	private NotificationSenderInt createSender() {
		return NotificationSenderInt.builder()
				.paId("TEST_PA_ID")
				.paTaxId("TEST_TAX_ID")
				.paDenomination("TEST_PA_DENOMINATION")
				.build();
	}
}
