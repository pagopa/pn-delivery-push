package it.pagopa.pn.deliverypush.actions;

import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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
	
	private final LegalFactPdfGeneratorUtils PDF_UTILS = new LegalFactPdfGeneratorUtils( timelineDao );
	private static final Duration ONE_HOUR = Duration.ofHours(1);
	private static final ZoneId ROME_ZONE = ZoneId.of("Europe/Rome");
	
	@BeforeEach
    public void setup() {
		pdfUtils = Mockito.mock(LegalFactPdfGeneratorUtils.class);
		timelineDao = Mockito.mock(TimelineDao.class);
    }
	
    @Test
    void testInstantToDateUnambiguousAmbiguousCest() {
        // - GIVEN
        Instant instant = Instant.parse("2021-10-30T23:59:00.000Z");
        
        // WHEN
        String convertedDate = PDF_UTILS.instantToDate( instant );
        
        // THEN
        Assertions.assertEquals("31/10/2021 01:59", convertedDate);
    }
    
    @Test
    void testInstantToDateAmbiguousCest() {
        // - GIVEN
        Instant instant = Instant.parse("2021-10-31T00:00:00.000Z");
        
        // WHEN
        String convertedDate = PDF_UTILS.instantToDate( instant );
        
        // THEN
        Assertions.assertEquals("31/10/2021 02:00 CEST", convertedDate);
    }
    
    @Test
    void testInstantToDateAmbiguousCet() {
        // - GIVEN
        Instant instant = Instant.parse("2021-10-31T02:00:00.000Z");
        
        // WHEN
        String convertedDate = PDF_UTILS.instantToDate( instant );
        
        // THEN
        Assertions.assertEquals("31/10/2021 03:00 CET", convertedDate);
    }
    
    @Test
    void testInstantToDateUnambiguousAmbiguousCet() {
        // - GIVEN
        Instant instant = Instant.parse("2021-10-31T02:01:00.000Z");
        
        // WHEN
        String convertedDate = PDF_UTILS.instantToDate( instant );
        
        // THEN
        Assertions.assertEquals("31/10/2021 03:01", convertedDate);
    }
    
    @Test
    void testInstantToDateAmbiguousCestCet() {
        // - GIVEN
        Instant instant1 = Instant.parse("2021-10-31T00:30:00.000Z");
        Instant instant2 = Instant.parse("2021-10-31T01:30:00.000Z");
        
        // WHEN
        String convertedDate1 = PDF_UTILS.instantToDate( instant1 );
        String convertedDate2 = PDF_UTILS.instantToDate( instant2 );
        
        // THEN
        Assertions.assertEquals("31/10/2021 02:30 CEST", convertedDate1);
        Assertions.assertEquals("31/10/2021 02:30 CET", convertedDate2);
    }
    
	@Test
	void testNotTimeZoneChangeDayInstantToDate() {
		// GIVEN
		Instant testDate = Instant.parse( "2021-10-11T09:55:00.000Z" );
		
		// WHEN
		String convertedDate = PDF_UTILS.instantToDate( testDate );
		
		// THEN
		Assertions.assertEquals("11/10/2021 11:55", convertedDate);
	}
	
	@Test 
	void testTrueIsNear() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// GIVEN
		Method privateMethod = LegalFactPdfGeneratorUtils.class.getDeclaredMethod( "isNear", Instant.class, Instant.class );
		privateMethod.setAccessible( true );
		Instant instant = Instant.parse("2021-10-31T00:00:00.000Z");
		Instant nextTransition = ROME_ZONE.getRules().nextTransition( instant ).getInstant();
		
		// WHEN
		boolean result = (boolean) privateMethod.invoke( PDF_UTILS, instant, nextTransition );
		
		// THEN
		Assertions.assertTrue(result);
	}
	
	@Test 
	void testFalseIsNear() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// GIVEN
		Method privateMethod = LegalFactPdfGeneratorUtils.class.getDeclaredMethod( "isNear", Instant.class, Instant.class );
		privateMethod.setAccessible( true );
		Instant instant = Instant.parse("2021-10-31T02:00:00.000Z");
		Instant nextTransition = ROME_ZONE.getRules().nextTransition( instant ).getInstant();
		
		// WHEN
		boolean result = (boolean) privateMethod.invoke( PDF_UTILS, instant, nextTransition );
		
		// THEN
		Assertions.assertFalse(result);
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
	
	@Test 
	void successNullSafePhysicalAddressToString() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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
		
		Method privateMethod = LegalFactPdfGeneratorUtils.class.getDeclaredMethod( "nullSafePhysicalAddressToString", NotificationRecipient.class );
		privateMethod.setAccessible( true );
		
		// WHEN
		String output = (String) privateMethod.invoke( PDF_UTILS, notification.getRecipients().get( 0 ) );
		output = String.join(";", output.split("\n"));
		
		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality province", output, "Different notification data");
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
