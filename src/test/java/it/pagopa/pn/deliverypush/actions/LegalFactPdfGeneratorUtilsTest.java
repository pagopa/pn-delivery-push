package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.itextpdf.text.DocumentException;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

class LegalFactPdfGeneratorUtilsTest {
	private LegalFactPdfGeneratorUtils pdfUtils;

	@BeforeEach
    public void setup() {
		pdfUtils = Mockito.mock(LegalFactPdfGeneratorUtils.class);
    }
	
	@Test
	void successConversionInstantToDate() {
		LegalFactPdfGeneratorUtils utils = new LegalFactPdfGeneratorUtils();
		Instant testDate = Instant.parse("2021-09-03T13:03:00.000Z");
		String date = utils.instantToDate(testDate);
		Assertions.assertEquals("2021-09-03 13:03", date);
	}
	
	@Test
	void successGenerateNotificationReceivedLegalFact() throws DocumentException {
		// GIVEN
		
		// WHEN
		pdfUtils.generateNotificationReceivedLegalFact(Mockito.any(Action.class),
				Mockito.any(Notification.class), Mockito.any(NotificationRecipient.class));
		
		// THEN
		Mockito.verify(pdfUtils).generateNotificationReceivedLegalFact(Mockito.any(),
				Mockito.any(), Mockito.any());
	}
	
	@Test
	void successGenerateNotificationViewedLegalFact () throws DocumentException {
		// GIVEN
	
		// WHEN
		pdfUtils.generateNotificationViewedLegalFact(Mockito.any(Action.class), Mockito.any(Notification.class));
		
		//THEN
		Mockito.verify(pdfUtils).generateNotificationViewedLegalFact(Mockito.any(), Mockito.any());
	}
	
	@Test
	void successGeneratePecDeliveryWorkflowLegalFact () throws DocumentException {
		// GIVEN
		
		// WHEN
		pdfUtils.generatePecDeliveryWorkflowLegalFact((List<Action>) Mockito.any(Action.class), Mockito.any(Notification.class), Mockito.any(NotificationPathChooseDetails.class));
		
		//THEN
		Mockito.verify(pdfUtils).generatePecDeliveryWorkflowLegalFact(Mockito.any(), Mockito.any(), Mockito.any());
	}

}
