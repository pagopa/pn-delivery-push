package it.pagopa.pn.deliverypush.legalfacts;

import java.util.Arrays;

import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfo;

class OpenhtmltopdfLegalFactPdfGeneratorTest {
	private OpenhtmltopdfLegalFactPdfGenerator pdfUtils;
	private TimelineDao timelineDao;
	
	@BeforeEach
    public void setup() {
		timelineDao = Mockito.mock(TimelineDao.class);
		pdfUtils = new OpenhtmltopdfLegalFactPdfGenerator( timelineDao );
    }
	


	@Test 
	void successHashUnorderedList() {
		// GIVEN
		Notification notification = Notification.builder()
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
					.payment( NotificationPaymentInfo.builder()
								.f24( NotificationPaymentInfo.F24.builder()
										.digital( NotificationAttachment.builder()
												.body("Ym9keV8wMQ==")
												.contentType("Content/Type")
												.digests(NotificationAttachment.Digests.builder()
														.sha256("sha256_doc02")
														.build() )
												.build() )
										.analog( NotificationAttachment.builder()
												.body("Ym9keV8wMQ==")
												.contentType("Content/Type")
												.digests(NotificationAttachment.Digests.builder()
														.sha256("sha256_doc03")
														.build() )
												.build() )
										.flatRate( NotificationAttachment.builder()
												.body("Ym9keV8wMQ==")
												.contentType("Content/Type")
												.digests(NotificationAttachment.Digests.builder()
														.sha256("sha256_doc04")
														.build() )
												.build() )
										.build() 
								)
								.build()
					)
					.build();
					
		// WHEN
		StringBuilder list = pdfUtils.hashUnorderedList( notification );
		String output = list.toString();
		
		// THEN
		Assertions.assertTrue(output.contains("<li>sha256_doc01;</li>"), "Different unordered list item");
		Assertions.assertTrue(output.contains("<li>sha256_doc02;</li>"), "Different unordered list item");
		Assertions.assertTrue(output.contains("<li>sha256_doc03;</li>"), "Different unordered list item");
		Assertions.assertTrue(output.contains("<li>sha256_doc04;</li>"), "Different unordered list item");
	}

}
