package it.pagopa.pn.deliverypush.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.legalfacts.DigitalAdviceReceiptLegalFact;
import it.pagopa.pn.api.dto.legalfacts.DigitalAdviceReceiptLegalFact.OkOrFail;
import it.pagopa.pn.api.dto.legalfacts.NotificationReceivedLegalFact;
import it.pagopa.pn.api.dto.legalfacts.NotificationViewedLegalFact;
import it.pagopa.pn.api.dto.legalfacts.RecipientInfo;
import it.pagopa.pn.api.dto.legalfacts.RecipientInfoWithAddresses;
import it.pagopa.pn.api.dto.legalfacts.SenderInfo;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

@Component
public class LegalFactUtils {
    private final ConcurrentHashMap<Class<?>, ObjectWriter> mapObjWriter = new ConcurrentHashMap<>();
    private final FileStorage fileStorage;
    private final ObjectMapper objMapper;

    public LegalFactUtils(FileStorage fileStorage, ObjectMapper objMapper) {
        this.fileStorage = fileStorage;
        this.objMapper = objMapper;
    }

    public void saveLegalFact(String iun, String name, Object legalFact) {
        try {
            ObjectWriter writer = mapObjWriter.computeIfAbsent(legalFact.getClass(), objMapper::writerFor);
            String bodyString = writer.writeValueAsString(legalFact);
            String key = iun + "/legalfacts/" + name + ".json";
            Map<String, String> metadata = Collections.singletonMap("Content-Type", "application/json; charset=utf-8");

            byte[] body = bodyString.getBytes(StandardCharsets.UTF_8);
            try (InputStream bodyStream = new ByteArrayInputStream(body)) {
                fileStorage.putFileVersion(key, bodyStream, body.length, metadata);
            }
        } catch (IOException exc) {
            throw new PnInternalException("Generating legal fact", exc);
        }
    }

	public void saveNotificationReceivedLegalFact(Action action, Notification notification, NotificationRecipient recipient) {
		DigitalAddress digitalDomicile = recipient.getDigitalDomicile();

		this.saveLegalFact( action.getIun(), "sender_ack_" + recipient.getTaxId(),
				NotificationReceivedLegalFact.builder()
						.iun( action.getIun() )
						.sender( SenderInfo.builder()
								.paTaxId( notification.getSender().getPaId() )
								.paDenomination( notification.getSender().getPaDenomination() )
								.build()
						)
						.date( this.instantToDate( notification.getSentAt() ))
						.recipient( RecipientInfoWithAddresses.builder()
								.taxId( recipient.getTaxId() )
								.denomination( recipient.getDenomination() )
								.digitalDomicile( digitalDomicile )
								.physicalDomicile( nullSafePhysicalAddressToString(recipient) )
								.build()
						)
						.digests( notification.getDocuments()
								.stream()
								.map( d -> d.getDigests().getSha256() )
								.collect(Collectors.toList()) )
						.build()
		);
	}

    public String instantToDate(Instant instant) {
        OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);
        int year = odt.get(ChronoField.YEAR_OF_ERA);
        int month = odt.get(ChronoField.MONTH_OF_YEAR);
        int day = odt.get(ChronoField.DAY_OF_MONTH);
		int hour = odt.get(ChronoField.HOUR_OF_DAY);
		int min = odt.get(ChronoField.MINUTE_OF_HOUR);
        return String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, min);
    }
    
    public void savePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses ) {

    	Set<Integer> recipientIdx = actions.stream()
				.map(a -> a.getRecipientIndex() )
				.collect(Collectors.toSet());
    	if( recipientIdx.size() > 1 ) {
    		throw new PnInternalException("Impossible generate distinct act for distinct recipients");
		}

    	List<DigitalAdviceReceiptLegalFact> legalFacts = actions.stream()
				.map( receivePecAction -> {
    				DigitalAddress address = receivePecAction.getDigitalAddressSource().getAddressFrom( addresses );
    				return buildDigitalAdviceReceiptLegalFact(receivePecAction, notification, address);
    			})
				.collect( Collectors.toList() );

    	String taxId = notification.getRecipients().get( recipientIdx.iterator().next() ).getTaxId();
    	this.saveLegalFact( notification.getIun(), "digital_delivery_info_" + taxId,
				legalFacts.toArray( new DigitalAdviceReceiptLegalFact[0] )
			);
    }

	private DigitalAdviceReceiptLegalFact buildDigitalAdviceReceiptLegalFact(Action action, Notification notification, DigitalAddress address) {
		PnExtChnProgressStatus status = action.getResponseStatus();
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
        
		return DigitalAdviceReceiptLegalFact.builder()
			.iun( notification.getIun() )
			.date( this.instantToDate( Instant.now() ) )
			.outcome( PnExtChnProgressStatus.OK.equals( status ) ? OkOrFail.OK : OkOrFail.FAIL )
			.recipient( RecipientInfo.builder()
		            		.taxId( recipient.getTaxId() )
		            		.denomination( recipient.getDenomination() )
		            		.build()
		    )
			.digitalAddress( address )
			.build();
	}
    
	private String nullSafePhysicalAddressToString( NotificationRecipient recipient ) {
		String result = null;
		
		if ( recipient != null ) {
			PhysicalAddress physicalAddress = recipient.getPhysicalAddress();
			if ( physicalAddress != null ) {
				List<String> standardAddressString = physicalAddress.toStandardAddressString( recipient.getDenomination() );
				if ( standardAddressString != null ) {
					result = String.join("\n", standardAddressString );
				}
			}
		}
		
		return result;
	}
	
    public void saveNotificationViewedLegalFact(Action action, Notification notification) {
    	NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
        
        NotificationViewedLegalFact notificationViewedLegalFact =  NotificationViewedLegalFact.builder()
		.iun( notification.getIun() )
		.date( this.instantToDate( Instant.now() ) )
		.recipient( RecipientInfo.builder()
        				.taxId( recipient.getTaxId() )
        				.denomination( recipient.getDenomination() )
        				.build() 
        )
		.build();
        
    	this.saveLegalFact( notification.getIun(), "notification_viewed_" + notification.getRecipients().get(0).getTaxId(), 
    							notificationViewedLegalFact);
    }
    	
}
