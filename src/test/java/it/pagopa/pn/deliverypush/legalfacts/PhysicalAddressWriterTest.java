package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;


class PhysicalAddressWriterTest {

	private PhysicalAddressWriter physicalAddressWriter;

	@BeforeEach
	public void setup() {
		physicalAddressWriter = new PhysicalAddressWriter();
	}


	@Test
	void successNullSafePhysicalAddressToString() {
		// GIVEN
		NotificationInt notification = NotificationInt.builder()
				.recipients( Collections.singletonList(
						NotificationRecipientInt.builder()
								.denomination( "denomination" )
								.physicalAddress(PhysicalAddressInt.builder()
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
		NotificationRecipientInt recipient = notification.getRecipients().get( 0 );
		String output = physicalAddressWriter.nullSafePhysicalAddressToString( recipient.getPhysicalAddress(), recipient.getDenomination(), ";" );

		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality province", output, "Different notification data");
	}
}