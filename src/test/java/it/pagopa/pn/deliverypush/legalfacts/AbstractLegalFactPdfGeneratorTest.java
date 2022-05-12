package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;


class AbstractLegalFactPdfGeneratorTest {
	private AbstractLegalFactPdfGenerator pdfUtils;
	private TimelineDao timelineDao;
	
	@BeforeEach
    public void setup() {
		timelineDao = Mockito.mock(TimelineDao.class);
		pdfUtils = new AbstractLegalFactPdfGenerator( timelineDao ) {};
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
		NotificationInt notification = NotificationInt.builder()
										.recipients( Collections.singletonList(
											NotificationRecipientInt.builder()
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
		String output = pdfUtils.nullSafePhysicalAddressToString( notification.getRecipients().get( 0 ), ";" );

		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality province", output, "Different notification data");
	}


}
