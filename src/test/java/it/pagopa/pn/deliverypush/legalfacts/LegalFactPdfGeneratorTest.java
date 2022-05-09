package it.pagopa.pn.deliverypush.legalfacts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import freemarker.template.Configuration;
import freemarker.template.Version;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews.New;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationRecipientTestBuilder;

class LegalFactPdfGeneratorTest {
	private DocumentComposition documentComposition;
    private CustomInstantWriter instantWriter;
    private PhysicalAddressWriter physicalAddressWriter;
    private LegalFactGenerator pdfUtils;
	
	@BeforeEach
    public void setup() throws IOException {
		Configuration freemarker = new Configuration(new Version(2,3,0));
		documentComposition = new DocumentComposition(freemarker);
		instantWriter = new CustomInstantWriter();
		physicalAddressWriter = Mockito.mock(PhysicalAddressWriter.class);
		
		pdfUtils = new LegalFactGenerator(documentComposition, instantWriter, physicalAddressWriter);
    }

	@Test 
	void successProduceReceivedLegalFactPDF() throws IOException {		
		String filePath = "C:\\Workspaces\\PagoPA\\test_ReceivedLegalFact.pdf";
		
		Path path = Paths.get(filePath );
		Files.write(path, pdfUtils.generateNotificationReceivedLegalFact(buildNotification()));		
		
		System.out.print("*** ReceivedLegalFact pdf successfully created at: " + filePath);
	}
	
	private Notification buildNotification() {
		return Notification.builder().sender(createSender("paIdTest"))
				.sentAt(Instant.now())
				.iun("iun1234Test")
				.documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .ref( NotificationAttachment.Ref.builder()
                                        .key("doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .contentType("application/pdf")
                                .body("Ym9keV8wMQ==")
                                .build()
							)
					)
				.recipients(buildRecipients())
				.build();
	}

	private List<NotificationRecipient> buildRecipients() {
		NotificationRecipient rec1 = NotificationRecipientTestBuilder.builder()
				.withTaxId("taxIdTest1234")
				.withPhysicalAddress(new PhysicalAddress("atTest", "addressTest", "addDetailTest", "ziptest", "munTest", "provTest", "NO"))
				.build();
		
		List<@NotNull(groups = New.class) @Valid NotificationRecipient> list = new ArrayList<NotificationRecipient>();
		list.add(rec1);
		return list;
	}

	private NotificationSender createSender(String paId) {
		return NotificationSender.builder().paId(paId).build();
	}
}
