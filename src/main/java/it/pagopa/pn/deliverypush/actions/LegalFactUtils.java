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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.legalfacts.DigitalAdviceReceiptLegalFact;
import it.pagopa.pn.api.dto.legalfacts.DigitalAdviceReceiptLegalFact.OkOrFail;
import it.pagopa.pn.api.dto.legalfacts.NotificationReceivedLegalFact;
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

    public String instantToDate(Instant instant) {
        OffsetDateTime odt = instant.atOffset(ZoneOffset.UTC);
        int year = odt.get(ChronoField.YEAR_OF_ERA);
        int month = odt.get(ChronoField.MONTH_OF_YEAR);
        int day = odt.get(ChronoField.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", year, month, day);
    }
    
    public void groupLegalFacts( List<Action> actions, Notification notification, Optional<NotificationPathChooseDetails> addresses ) {
    	List<DigitalAdviceReceiptLegalFact> legalFacts = actions.stream().map( a -> { 
    		DigitalAddress address = a.getDigitalAddressSource().getAddressFrom( addresses.get() );
    		return buildDigitalAdviceReceiptLegalFact(a, notification, address);
    	}).collect( Collectors.toList() ); 
    	
    	this.saveLegalFact( notification.getIun(), "digital_delivery_info.json", legalFacts.toArray( new DigitalAdviceReceiptLegalFact[0] ) );
    }
    
    public void notificationReceivedLegalFact(Action action, Notification notification) {
		for( NotificationRecipient recipient: notification.getRecipients() ) {
            this.saveLegalFact( action.getIun(), "sender_ack_" + recipient.getTaxId(),
                    NotificationReceivedLegalFact.builder()
                            .iun( notification.getIun() )
                            .sender( SenderInfo.builder()
                                    .paTaxId( notification.getSender().getPaId() )
                                    .paDenomination( notification.getSender().getPaDenomination() )
                                    .build()
                            )
                            .date( this.instantToDate( notification.getSentAt() ))
                            .recipient( RecipientInfoWithAddresses.builder()
                                    .taxId( recipient.getTaxId() )
                                    .denomination( recipient.getDenomination() )
                                    .digitalDomicile( recipient.getDigitalDomicile().getAddress() ) //FIXME : il domicilio digitale diventera facoltativo
                                    .digitalAddressType( recipient.getDigitalDomicile().getType() ) //FIXME : il domicilio digitale diventera facoltativo
                                    .physicalDomicile( nullSafePhysicalAddressToString( recipient ) )
                                    .build()
                            )
                            .digests( notification.getDocuments()
                                    .stream()
                                    .map( d -> d.getDigests().getSha256() )
                                    .collect(Collectors.toList()) )
                            .build()
            );
        }
	}
    
	public void digitalAdviceReceiptLegalFact(Action action, Notification notification, Optional<NotificationPathChooseDetails> addresses) {
	
		if( addresses.isPresent() ) {
			DigitalAddress address = action.getDigitalAddressSource().getAddressFrom( addresses.get() );
        	
	        this.saveLegalFact( action.getIun(), "sent_pec_" + action.getRecipientIndex(),
	        		buildDigitalAdviceReceiptLegalFact(action, notification, address )
	        );
        } else {
            throw new PnInternalException( "Digital Addresses not found!!! Cannot generate digital advice receipt" );
        }
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
			.digitalAddress( address.getAddress() )
			.digitalAddressType( address.getType() )
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
}
