package it.pagopa.pn.deliverypush.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.itextpdf.text.DocumentException;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

@Component
public class LegalFactUtils {
    private final FileStorage fileStorage;
    private final LegalFactPdfGeneratorUtils pdfUtils;

    public LegalFactUtils(FileStorage fileStorage,
    		LegalFactPdfGeneratorUtils pdfUtils) {
        this.fileStorage = fileStorage;
        this.pdfUtils = pdfUtils;
    }
    
    public void saveLegalFact(String iun, String name, byte[] legalFact) {
        try {
            String key = iun + "/legalfacts/" + name + ".pdf";
            Map<String, String> metadata = Collections.singletonMap("Content-Type", "application/pdf; charset=utf-8");

            try (InputStream bodyStream = new ByteArrayInputStream(legalFact)) {
                fileStorage.putFileVersion(key, bodyStream, legalFact.length, metadata);
            }
        } catch (IOException exc) {
            throw new PnInternalException("Generating legal fact", exc);
        }
    }

	public void saveNotificationReceivedLegalFact(Action action, Notification notification, NotificationRecipient recipient) {		
		try {
    		byte[] pdfBytes = pdfUtils.generateNotificationReceivedLegalFact( action, notification, recipient );
    		this.saveLegalFact(action.getIun(), "sender_ack_" + recipient.getTaxId(), pdfBytes);
		} catch (DocumentException exc) {
			throw new PnInternalException("Generating legal fact", exc);
		}
	}

//    public String instantToDate(Instant instant) {
//        OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);
//        int year = odt.get(ChronoField.YEAR_OF_ERA);
//        int month = odt.get(ChronoField.MONTH_OF_YEAR);
//        int day = odt.get(ChronoField.DAY_OF_MONTH);
//		int hour = odt.get(ChronoField.HOUR_OF_DAY);
//		int min = odt.get(ChronoField.MINUTE_OF_HOUR);
//        return String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, min);
//    }
    
    public void savePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses ) {

    	Set<Integer> recipientIdx = actions.stream()
				.map( Action::getRecipientIndex )
				.collect(Collectors.toSet());
    	if( recipientIdx.size() > 1 ) {
    		throw new PnInternalException("Impossible generate distinct act for distinct recipients");
		}
    	    	
    	String taxId = notification.getRecipients().get( recipientIdx.iterator().next() ).getTaxId();
    	
    	try {
    		byte[] pdfBytes = pdfUtils.generatePecDeliveryWorkflowLegalFact( actions, notification, addresses );
    		this.saveLegalFact(notification.getIun(), "digital_delivery_info_" + taxId, pdfBytes);
		} catch (DocumentException exc) {
			throw new PnInternalException("Generating legal fact...", exc);
		}
    }
    
    public void saveNotificationViewedLegalFact(Action action, Notification notification) {
    	try {
    		byte[] pdfBytes = pdfUtils.generateNotificationViewedLegalFact( action, notification );
    		this.saveLegalFact(notification.getIun(), "notification_viewed_" + notification.getRecipients().get(0).getTaxId(), pdfBytes);
		} catch (DocumentException exc) {
			throw new PnInternalException("Generating legal fact...", exc);
		}
    }
        	
}
