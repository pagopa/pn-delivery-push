package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfo;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;

class LegalFactPdfFromHtmlGeneratorUtilsTest {
	private LegalFactPdfFromHtmlGeneratorUtils pdfUtils;
	private TimelineDao timelineDao;
	
	@BeforeEach
    public void setup() {
		timelineDao = Mockito.mock(TimelineDao.class);
		pdfUtils = new LegalFactPdfFromHtmlGeneratorUtils( timelineDao );
    }
	
    @ParameterizedTest
	@CsvSource({
			"2021-10-31T00:30:00.000Z, 31/10/2021 02:30 CEST",
			"2021-10-31T01:30:00.000Z, 31/10/2021 02:30 CET",
			"2021-10-30T23:59:00.000Z, 31/10/2021 01:59",
			"2021-10-31T00:00:00.000Z, 31/10/2021 02:00 CEST",
			"2021-10-31T02:00:00.000Z, 31/10/2021 03:00 CET",
			"2021-10-31T02:01:00.000Z, 31/10/2021 03:01",
			"2021-10-11T09:55:00.000Z, 11/10/2021 11:55"
	})
    void testInstantToDateConversion(String isoZuluTimeInstant, String expected) {
        // GIVEN
        Instant instant = Instant.parse(isoZuluTimeInstant);
        
        // WHEN
        String convertedDate = pdfUtils.instantToDate( instant );
        
        // THEN
        Assertions.assertEquals(expected, convertedDate);
    }

	@Test 
	void successNullSafePhysicalAddressToString() {
		// GIVEN
		Notification notification = Notification.builder()
										.recipients( Collections.singletonList(
											NotificationRecipient.builder()
												.denomination( "denomination" )
												.physicalAddress(PhysicalAddress.builder()
													.address( "address" )
													.municipality( "municipality" )
													.addressDetails( "addressDetail" )
													.at( "at" )
													.province( "province" )
													.zip( "zip" )
													.build()
												).build() 
											) 
										).build();
		

		// WHEN
		String output = pdfUtils.nullSafePhysicalAddressToString( notification.getRecipients().get( 0 ) );
		output = String.join(";", output.split("<br/>"));
		
		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality province", output, "Different notification data");
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
