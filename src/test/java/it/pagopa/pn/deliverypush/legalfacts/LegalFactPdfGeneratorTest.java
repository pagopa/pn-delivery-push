package it.pagopa.pn.deliverypush.legalfacts;

import freemarker.template.Configuration;
import freemarker.template.Version;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

class LegalFactPdfGeneratorTest {
	private static final String TEST_DIR_NAME = "target" + File.separator + "generated-test-PDF";
	private static final Path TEST_DIR_PATH = Paths.get(TEST_DIR_NAME);

	private DocumentComposition documentComposition;
	private CustomInstantWriter instantWriter;
	private PhysicalAddressWriter physicalAddressWriter;
	private LegalFactGenerator pdfUtils;

	@BeforeEach
	public void setup() throws IOException {
		Configuration freemarker = new Configuration(new Version(2,3,0)); //Version is a final class
		documentComposition = new DocumentComposition(freemarker);
		
		instantWriter = new CustomInstantWriter();
		physicalAddressWriter = new PhysicalAddressWriter();

		pdfUtils = new LegalFactGenerator(documentComposition, instantWriter, physicalAddressWriter);

		//create target test folder, if not exists
		if (Files.notExists(TEST_DIR_PATH)) { 
			Files.createDirectory(TEST_DIR_PATH);
		}
	}

	@Test 
	void generateNotificationReceivedLegalFactTest() throws IOException {	
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ReceivedLegalFact.pdf");
		Files.write(filePath, pdfUtils.generateNotificationReceivedLegalFact(buildNotification()));		
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generateNotificationViewedLegalFactTest() throws IOException {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_ViewedLegalFact.pdf");
		String iun = "iun1234Test_Viewed";
		NotificationRecipientInt recipient = buildRecipients().get(0);
		Files.write(filePath, pdfUtils.generateNotificationViewedLegalFact(iun, recipient, Instant.now()));		
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generatePecDeliveryWorkflowLegalFactTest() throws IOException {
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_PecDeliveryWorkflowLegalFact.pdf");
		List<SendDigitalFeedback> feedbackFromExtChannelList = buildFeedbackFromECList();
		NotificationInt notification = buildNotification();
		NotificationRecipientInt recipient = buildRecipients().get(0);
		Files.write(filePath, pdfUtils.generatePecDeliveryWorkflowLegalFact(feedbackFromExtChannelList, notification, recipient));
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	@Test 
	void generategenerateFileComplianceTest() throws IOException {	
		Path filePath = Paths.get(TEST_DIR_NAME + File.separator + "test_FileCompliance.pdf");
		Files.write(filePath, pdfUtils.generateFileCompliance("PDF file name whitout extension", "test signature", Instant.now()));		
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}

	private List<SendDigitalFeedback> buildFeedbackFromECList() {
		SendDigitalFeedback sdf = SendDigitalFeedback.builder()
				.recIndex( 0 )
				.digitalAddress(DigitalAddress.builder()
						.type(DigitalAddress.TypeEnum.PEC)
						.address("indirizzo di prova test")
						.build())
				.responseStatus(ResponseStatus.OK)
				.notificationDate( Instant.now() )
				.build();
	
		
		List<SendDigitalFeedback> result = new ArrayList<SendDigitalFeedback>();
		result.add(sdf);
		return result;
	}

	private NotificationInt buildNotification() {
		return NotificationInt.builder().sender(createSender("paIdTest"))
				.sentAt(Instant.now())
				.iun("iun1234Test_buildNot")
				.documents(Arrays.asList(
						NotificationDocumentInt.builder()
							.ref( NotificationDocumentInt.Ref.builder()
									.key("doc00")
									.versionToken("v01_doc00")
									.build()
							)
							.digests(NotificationDocumentInt.Digests.builder()
									.sha256("sha256_doc01")
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
				.digitalDomicile(DigitalAddress.builder()
						.address("test@dominioPec.it")
						.type(DigitalAddress.TypeEnum.PEC)
						.build())
				.physicalAddress(new PhysicalAddress(
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

	private NotificationSenderInt createSender(String paId) {
		return NotificationSenderInt.builder().paId(paId).build();
	}
}
