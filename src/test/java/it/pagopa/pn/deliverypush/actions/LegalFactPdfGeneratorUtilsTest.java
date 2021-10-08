package it.pagopa.pn.deliverypush.actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.itextpdf.text.DocumentException;

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
		pdfUtils = Mockito.mock(LegalFactPdfGeneratorUtils.class);
		timelineDao = Mockito.mock(TimelineDao.class);
    }
	
	@Test //TODO rimuovi commenti
	void successConversionInstantToDate() {
		// GIVEN
		LegalFactPdfGeneratorUtils utils = new LegalFactPdfGeneratorUtils( timelineDao );
		
		ZoneId zoneId = ZoneId.of( "Europe/Rome" );
		
		Instant testDateUTC = Instant.parse( "2021-03-28T01:55:00.000Z" );
		Instant actualDateUTC = Instant.now();
		
		// WHEN
		LocalDateTime dateBeforeDST = LocalDateTime.ofInstant( testDateUTC, ZoneOffset.UTC );
		ZonedDateTime zonedDateBeforeDST = dateBeforeDST.atZone( zoneId );
		String convertedDateBeforeDST = utils.instantToDate( testDateUTC ); 
		
		ZonedDateTime zonedDateAfterDST = zonedDateBeforeDST.plus( 10, ChronoUnit.MINUTES );
		String convertedDateAfterDST = utils.instantToDate( Instant.parse( "2021-03-28T02:05:00.000Z" ) );
		
		String actualConvertedDate = utils.instantToDate( actualDateUTC );
    	LocalDateTime actualLocalDate = LocalDateTime.ofInstant(actualDateUTC, zoneId);
		String actualZonedDate = actualLocalDate.format(DateTimeFormatter.ofPattern( "dd/MM/yyyy HH:mm" ) );
		
		// THEN
		Assertions.assertEquals( "2021-03-28T01:55+01:00[Europe/Rome]", zonedDateBeforeDST.toString() );
		Assertions.assertEquals( "2021-03-28T03:05+02:00[Europe/Rome]", zonedDateAfterDST.toString() );
		
		Assertions.assertEquals( "28/03/2021 03:55", convertedDateBeforeDST );
		Assertions.assertEquals( "28/03/2021 04:05", convertedDateAfterDST );
		
		Assertions.assertEquals(actualZonedDate, actualConvertedDate);
	}
	
	@Test
	void successGenerateNotificationReceivedLegalFact() throws DocumentException {
		// GIVEN
        Notification notification = newNotification();
        Action action = Action.builder()
                .iun( notification.getIun() )
                .recipientIndex(0)
                .type(ActionType.SENDER_ACK)
                .retryNumber(1)
                .notBefore(Instant.now())
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .actionId("Test_iun01_send_pec_rec0_null_nnull")
                .build();
        String actionId = action.getType().buildActionId(action);
        action = action.toBuilder().actionId(actionId).build();
        byte[] byteArray = new byte[] { 77, 97, 114, 121 };
        
		// WHEN
		when(pdfUtils.generateNotificationReceivedLegalFact( action, notification )).thenReturn( byteArray );
		
		// THEN
		byte[] newByteArray = pdfUtils.generateNotificationReceivedLegalFact( action, notification );
		Assertions.assertEquals( byteArray, newByteArray, "Different byteArray");
		
		ArgumentCaptor<Action> actionCapture = ArgumentCaptor.forClass(Action.class);
        ArgumentCaptor<Notification> notificationCapture = ArgumentCaptor.forClass(Notification.class);

        Mockito.verify( pdfUtils, Mockito.times(1)).generateNotificationReceivedLegalFact( actionCapture.capture(),
                notificationCapture.capture() );
		Mockito.verifyNoMoreInteractions( pdfUtils );
		
		Assertions.assertEquals(action.getIun(), actionCapture.getValue().getIun(), "Different iun");
        Assertions.assertEquals( action.getIun(), notificationCapture.getValue().getIun(), "Different iun");
	}
	
	@Test
	void successGenerateNotificationViewedLegalFact () throws DocumentException {
		// GIVEN
		Notification notification = newNotification();
        Action action = Action.builder()
                .iun( notification.getIun() )
                .recipientIndex(0)
                .type(ActionType.NOTIFICATION_VIEWED)
                .retryNumber(1)
                .notBefore(Instant.now())
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .actionId("Test_iun01_notification_viewed0")
                .build();
        String actionId = action.getType().buildActionId(action);
        action = action.toBuilder().actionId(actionId).build();
        byte[] byteArray = new byte[] { 77, 97, 114, 121 };
        
		// WHEN
		when(pdfUtils.generateNotificationViewedLegalFact( action, notification )).thenReturn( byteArray );
		
		//THEN
		byte[] newByteArray = pdfUtils.generateNotificationViewedLegalFact( action, notification );
		Assertions.assertEquals( byteArray, newByteArray, "Different byteArray");
		
		ArgumentCaptor<Action> actionCapture = ArgumentCaptor.forClass(Action.class);
        ArgumentCaptor<Notification> notificationCapture = ArgumentCaptor.forClass(Notification.class);

        Mockito.verify( pdfUtils, Mockito.times(1)).generateNotificationViewedLegalFact( actionCapture.capture(),
                notificationCapture.capture() );
		Mockito.verifyNoMoreInteractions( pdfUtils );
		
		Assertions.assertEquals( action.getIun(), actionCapture.getValue().getIun(), "Different iun");
        Assertions.assertEquals( action.getIun(), notificationCapture.getValue().getIun(), "Different iun");
	}
	
	@Test
	void successGeneratePecDeliveryWorkflowLegalFact () throws DocumentException {
		// GIVEN
		Notification notification = newNotification();
        Action action = Action.builder()
                .iun( notification.getIun() )
                .recipientIndex(0)
                .type(ActionType.END_OF_DIGITAL_DELIVERY_WORKFLOW)
                .retryNumber(1)
                .notBefore(Instant.now())
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .actionId("Test_iun01_notification_viewed0")
                .build();
        String actionId = action.getType().buildActionId(action);
        action = action.toBuilder().actionId(actionId).build();
        
        byte[] byteArray = new byte[] { 77, 97, 114, 121 };
        
        NotificationPathChooseDetails addresses = NotificationPathChooseDetails.builder()
        											.physicalAddress( PhysicalAddress.builder()
        																.address( "address" )
        																.addressDetails( "adrressDetail" )
        																.at( "at" )
        																.province( "province" )
        																.zip( "zip" )
        																.build() )
        											.build();
        
        List<Action> actions = new ArrayList<Action>();
        actions.add( action );
        
		// WHEN
		when(pdfUtils.generatePecDeliveryWorkflowLegalFact( actions, notification, addresses )).thenReturn( byteArray );
		
		//THEN
		byte[] newByteArray = pdfUtils.generatePecDeliveryWorkflowLegalFact( actions, notification, addresses );
		Assertions.assertEquals( byteArray, newByteArray, "Different byteArray");
		
		@SuppressWarnings("unchecked")
		Class<ArrayList<Action>> listClass = (Class<ArrayList<Action>>)(Class)ArrayList.class;
		ArgumentCaptor<ArrayList<Action>> actionCapture = ArgumentCaptor.forClass(listClass);
	
        ArgumentCaptor<Notification> notificationCapture = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<NotificationPathChooseDetails> addressCapture = ArgumentCaptor.forClass(NotificationPathChooseDetails.class);

        Mockito.verify( pdfUtils, Mockito.times(1)).generatePecDeliveryWorkflowLegalFact( actionCapture.capture(),
                notificationCapture.capture(),  addressCapture.capture() );
		Mockito.verifyNoMoreInteractions( pdfUtils );
		
		Assertions.assertEquals( action.getIun(), actionCapture.getValue().get(0).getIun(), "Different iun");
        Assertions.assertEquals( action.getIun(), notificationCapture.getValue().getIun(), "Different iun");
        Assertions.assertEquals( addresses.getPhysicalAddress().getAddress(), addressCapture.getValue().getPhysicalAddress().getAddress(), "Different address");
	}
	
	private Notification newNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .ref( NotificationAttachment.Ref.builder()
										.key("doc00")
										.versionToken("v01_doc00")
										.build()
								)
								.digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .contentType("application/pdf")
                                .body("Ym9keV8wMQ==")
                                .build(),
                        NotificationAttachment.builder()
								.ref( NotificationAttachment.Ref.builder()
										.key("doc01")
										.versionToken("v01_doc01")
										.build()
								)
								.digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .contentType("application/pdf")
                                .body("Ym9keV8wMg==")
                                .build()
                ))
                .build();
    }

}
