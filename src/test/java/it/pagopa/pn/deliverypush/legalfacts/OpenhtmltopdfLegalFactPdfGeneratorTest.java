package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

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
		NotificationInt notification = getNotificationInt();
		
		// WHEN
		StringBuilder list = pdfUtils.hashUnorderedList( notification );
		String output = list.toString();
		
		// THEN
		Assertions.assertTrue(output.contains("<li>sha256_doc01;</li>"), "Different unordered list item");
		Assertions.assertTrue(output.contains("<li>sha256_doc02;</li>"), "Different unordered list item");
		Assertions.assertTrue(output.contains("<li>sha256_doc03;</li>"), "Different unordered list item");
		Assertions.assertTrue(output.contains("<li>sha256_doc04;</li>"), "Different unordered list item");
	}

	private NotificationInt getNotificationInt() {
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
                                .build()
							)
					)
					.recipients(Collections.singletonList(
							NotificationRecipientInt.builder()
									.taxId("testIdRecipient")
									.denomination("Nome Cognome/Ragione Sociale")
									.digitalDomicile(DigitalAddress.builder()
											.type(DigitalAddress.TypeEnum.PEC)
											.address("account@dominio.it")
											.build())
									.payment(
											NotificationPaymentInfoInt.builder()
													.f24flatRate(
															NotificationDocumentInt.builder()
																	.digests(
																			NotificationDocumentInt.Digests.builder()
																					.sha256("sha256_doc02")
																					.build()	
																	)
																	.build()
													)
													.pagoPaForm(
															NotificationDocumentInt.builder()
																	.digests(
																			NotificationDocumentInt.Digests.builder()
																					.sha256("sha256_doc03")
																					.build()
																	)
																	.build()
													)
													.f24white(
															NotificationDocumentInt.builder()
																	.digests(
																			NotificationDocumentInt.Digests.builder()
																					.sha256("sha256_doc04")
																					.build()
																	)
																	.build()
													)
													.build()	
									)
									.build()
					)).build();
		return notification;
	}

}
