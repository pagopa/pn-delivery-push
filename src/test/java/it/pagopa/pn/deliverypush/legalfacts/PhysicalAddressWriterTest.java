package it.pagopa.pn.deliverypush.legalfacts;

import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;


class PhysicalAddressWriterTest {

	private PhysicalAddressWriter physicalAddressWriter;

	@BeforeEach
	public void setup() {
		physicalAddressWriter = new PhysicalAddressWriter();
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
		NotificationRecipient recipient = notification.getRecipients().get( 0 );
		String output = physicalAddressWriter.nullSafePhysicalAddressToString( recipient.getPhysicalAddress(), recipient.getDenomination(), ";" );

		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality province", output, "Different notification data");
	}
}