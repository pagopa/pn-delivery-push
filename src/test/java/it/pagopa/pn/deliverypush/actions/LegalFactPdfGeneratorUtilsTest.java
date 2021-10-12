package it.pagopa.pn.deliverypush.actions;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;

class LegalFactPdfGeneratorUtilsTest {
	private LegalFactPdfGeneratorUtils pdfUtils;
	private TimelineDao timelineDao;
	
	@BeforeEach
    public void setup() {
		timelineDao = Mockito.mock(TimelineDao.class);
		pdfUtils = new LegalFactPdfGeneratorUtils( timelineDao );
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
		output = String.join(";", output.split("\n"));
		
		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality province", output, "Different notification data");
	}


}
