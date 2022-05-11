package it.pagopa.pn.deliverypush.legalfacts;
//TODO Da decommentare
/*
import java.util.Arrays;

import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
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
		NotificationInt notification = NotificationInt.builder()
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
                                .contentType("application/pdf")
                                .body("Ym9keV8wMQ==")
                                .build()
							)
					)
					.payment( NotificationPaymentInfo.builder()
								.f24( NotificationPaymentInfo.F24.builder()
										.digital( NotificationDocumentInt.builder()
												.body("Ym9keV8wMQ==")
												.contentType("Content/Type")
												.digests(NotificationDocumentInt.Digests.builder()
														.sha256("sha256_doc02")
														.build() )
												.build() )
										.analog( NotificationDocumentInt.builder()
												.body("Ym9keV8wMQ==")
												.contentType("Content/Type")
												.digests(NotificationDocumentInt.Digests.builder()
														.sha256("sha256_doc03")
														.build() )
												.build() )
										.flatRate( NotificationDocumentInt.builder()
												.body("Ym9keV8wMQ==")
												.contentType("Content/Type")
												.digests(NotificationDocumentInt.Digests.builder()
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

 */
