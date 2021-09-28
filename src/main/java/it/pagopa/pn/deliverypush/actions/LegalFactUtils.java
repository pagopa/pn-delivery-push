package it.pagopa.pn.deliverypush.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.itextpdf.text.DocumentException;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
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
    
    public void saveLegalFact(String iun, String name, byte[] legalFact, Map<String, String> metadata) {
    	String key = iun + "/legalfacts/" + name + ".pdf";
        try {
        	//Map<String, String> metadata = Collections.singletonMap("Content-Type", "application/pdf; charset=utf-8");
            try (InputStream bodyStream = new ByteArrayInputStream(legalFact)) {
                fileStorage.putFileVersion(key, bodyStream, legalFact.length, metadata);
            }
        } catch (IOException exc) {
        	String errMsg = "Error while saving file on storage: " + key + ".";
            throw new PnInternalException(errMsg, exc);
        }
    }

	public void saveNotificationReceivedLegalFact(Action action, Notification notification) {
		try {
			Map<String, String> metadata = metadata( LegalFactType.SENDER_ACK.name(), null );
			
    		byte[] pdfBytes = pdfUtils.generateNotificationReceivedLegalFact( action, notification);
    		this.saveLegalFact(action.getIun(), "sender_ack", pdfBytes, metadata);
		} catch (DocumentException exc) {
			String errMsg = "Error while generating legal fact \"Attestazione (lett. a, b)\" for Notification with Iun: " + notification.getIun() + ".";
			throw new PnInternalException(errMsg, exc);
		}
	}
    
    public void savePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses ) {
    	Set<Integer> recipientIdx = actions.stream()
				.map( Action::getRecipientIndex )
				.collect(Collectors.toSet());
    	if( recipientIdx.size() > 1 ) {
    		throw new PnInternalException("Impossible generate distinct act for distinct recipients");
		}
    	    	
    	try {
    		String taxId = notification.getRecipients().get( recipientIdx.iterator().next() ).getTaxId();
    		Map<String, String> metadata = metadata( LegalFactType.DIGITAL_DELIVERY.name(), taxId );
    		
    		byte[] pdfBytes = pdfUtils.generatePecDeliveryWorkflowLegalFact( actions, notification, addresses );
    		this.saveLegalFact( notification.getIun(), "digital_delivery_info_" + taxId, pdfBytes, metadata );
		} catch (DocumentException exc) {
			String errMsg = "Error while generating legal fact \"Attestazione (lett. c)\" for Notification with Iun: " + notification.getIun() + ".";
			throw new PnInternalException( errMsg, exc );
		}
    }
    
    public void saveNotificationViewedLegalFact(Action action, Notification notification) {
    	try {
    		String taxId = notification.getRecipients().get(0).getTaxId();
    		Map<String, String> metadata = metadata( LegalFactType.RECIPIENT_ACCESS.name(), taxId );
    		
    		byte[] pdfBytes = pdfUtils.generateNotificationViewedLegalFact( action, notification );
    		this.saveLegalFact( notification.getIun(), "notification_viewed_" + taxId, pdfBytes, metadata );
		} catch (DocumentException exc) {
			String errMsg = "Error while generating legal fact \"Attestazione (lett. e)\" for Notification with Iun: " + notification.getIun() + ".";
			throw new PnInternalException( errMsg, exc );
		}
    }
    
	private Map<String, String> metadata(String type, String taxId) {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("Content-Type", "application/pdf; charset=utf-8");
		
		if ( StringUtils.isNotBlank( type ) ) {
			metadata.put("type", type);
		}
		if ( StringUtils.isNotBlank( taxId ) ) {
			metadata.put("taxid", taxId);
		}

		return metadata;
	}
        	
}
