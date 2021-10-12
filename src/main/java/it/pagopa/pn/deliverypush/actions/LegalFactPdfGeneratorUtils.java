package it.pagopa.pn.deliverypush.actions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LegalFactPdfGeneratorUtils {

	private static final DateTimeFormatter ITALIAN_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final Duration ONE_HOUR = Duration.ofHours(1);
	private static final ZoneId ROME_ZONE = ZoneId.of("Europe/Rome");
    
	private final TimelineDao timelineDao;
	
	@Autowired
	public LegalFactPdfGeneratorUtils(TimelineDao timelineDao) {
        this.timelineDao = timelineDao;
    }
	
	private static final String PARAGRAPH1 = "Ai sensi dell’art. 26, comma 11, del decreto-legge,\n"
			+ "la PagoPA s.p.a. nella sua qualità di gestore ex lege\n"
			+ "della Piattaforma Notifiche Digitali di cui allo stesso art. 26,\n"
			+ "con ogni valore legale per l'opponibilità a terzi, ATTESTA CHE:\n";
    
	public byte[] generateNotificationReceivedLegalFact(Action action, Notification notification) {
		String paragraph2 = "in data %s il soggetto mittente %s, C.F. "
	    		+ "%s ha messo a disposizione del gestore i documenti informatici di "
	    		+ "cui allo IUN %s e identificati in modo univoco con i seguenti hash: ";
	    paragraph2 = String.format( paragraph2, this.instantToDate( notification.getSentAt() ),
	    										notification.getSender().getPaDenomination(),
	    										notification.getSender().getTaxId( notification.getSender().getPaId() ),
	    										action.getIun());
	    StringBuilder bld = new StringBuilder();
	    for (int idx = 0; idx < notification.getDocuments().size(); idx ++) {
	    	bld.append( notification.getDocuments().get(idx).getDigests().getSha256() + "; " );
	    }
	    
	    if ( notification.getPayment() != null && notification.getPayment().getF24() != null ) {
	    	if ( notification.getPayment().getF24().getFlatRate() != null) {
	    		bld.append( notification.getPayment().getF24().getFlatRate().getDigests().getSha256() + ";" );
	    	}
	    	if ( notification.getPayment().getF24().getDigital() != null) {
	    		bld.append( notification.getPayment().getF24().getDigital().getDigests().getSha256() + ";" );
	    	}
	    	if ( notification.getPayment().getF24().getAnalog() != null) {
	    		bld.append( notification.getPayment().getF24().getAnalog().getDigests().getSha256() + ";" );
	    	}
	    }
	    
	    paragraph2 = paragraph2 + bld.toString();
	    
	    String paragraph3 = "il soggetto mittente ha richiesto che la notificazione di tali documenti fosse eseguita nei "
	    		+ "confronti dei seguenti soggetti destinatari che in seguito alle verifiche di cui all’art. 7, commi "
	    		+ "1 e 2, del DPCM del - ........, sono indicati unitamente al loro domicilio digitale o in assenza al "
	    		+ "loro indirizzo fisico utile ai fini della notificazione richiesta:";

		List<String> paragraphs = new ArrayList<>();
		paragraphs.add( PARAGRAPH1 );
		paragraphs.add( paragraph2 );
		paragraphs.add( paragraph3 );

		for ( NotificationRecipient recipient : notification.getRecipients() ) {
		    paragraphs.add( String.format(
		    		"nome e cognome/ragione sociale %s, C.F. %s domicilio digitale %s, indirizzo fisico %s;",
					recipient.getDenomination(),
					recipient.getTaxId(),
					recipient.getDigitalDomicile().getAddress(),
					nullSafePhysicalAddressToString( recipient )
				));
		}

		return toPdfBytes( paragraphs );
	}


	public byte[] generateNotificationViewedLegalFact(Action action, Notification notification) {
		if (action.getRecipientIndex() == null) {
			String msg = "Error while retrieving RecipientIndex for IUN %s";
        	msg = String.format( msg, action.getIun() );
        	log.debug( msg );
        	throw new PnInternalException( msg );
		}
		
		NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
		TimelineElement row = timelineElement(action);
        
	    String paragraph2 = "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:";
	    paragraph2 = String.format( paragraph2, notification.getIun() );
	    
	    String paragraph3 = "nome e cognome/ragione sociale %s, C.F. %s "
	    		+ "domicilio digitale %s: in data %s il destinatario ha avuto "
	    		+ "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato.";
	    paragraph3 = String.format( paragraph3, recipient.getDenomination(), 
	    										recipient.getTaxId(), 
	    										recipient.getDigitalDomicile().getAddress(), 
	    										this.instantToDate( row.getTimestamp() ) );
	    
	    String paragraph4 = "Si segnala che ogni successivo accesso ai medesimi documenti non è oggetto della presente "
	    		+ "attestazione in quanto irrilevante ai fini del perfezionamento della notificazione.";

		return toPdfBytes( Arrays.asList( PARAGRAPH1, paragraph2, paragraph3, paragraph4));
	}
	
	public byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses) {

		List<String> paragraphs = new ArrayList<>();
		paragraphs.add( PARAGRAPH1 );
		String paragraph2 = String.format(
				"gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:",
				notification.getIun()
			);

		StringBuilder paragraph3 = new StringBuilder();
    	for (Action action : actions) {
	    	DigitalAddress address = action.getDigitalAddressSource().getAddressFrom( addresses );
	    	NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
	    	PnExtChnProgressStatus status = action.getResponseStatus();
	       	    	
	    	paragraph3.append( String.format(
	    			"nome e cognome/ragione sociale %s, C.F. %s con domicilio digitale %s: ",
					recipient.getDenomination(),
	    			recipient.getTaxId(),
	    			address.getAddress()
				));
    		 
	    	TimelineElement row = timelineElement(action);
	    	Instant timestamp = row.getTimestamp();
	    	 
	    	if (PnExtChnProgressStatus.OK.equals( status )) {
				paragraph3.append( String.format(
						"il relativo avviso di avvenuta ricezione in formato elettronico è stato consegnato in data %s",
						this.instantToDate( timestamp )
					));
	    	} else {
				paragraph3.append( String.format(
						"in data %s è stato ricevuto il relativo messaggio di mancato recapito al domicilio digitale già indicato.",
						this.instantToDate( timestamp )
					));
	    	}
		}

		return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3.toString()));
	}

	private byte[] toPdfBytes( List<String> paragraphs) throws PnInternalException {

		try (PDDocument doc = new PDDocument() ) {
			PDPage page = new PDPage();
			doc.addPage(page);
			PDFont font = PDType1Font.HELVETICA;

			try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
				contents.beginText();
				contents.setFont(font, 12);
				contents.newLineAtOffset(50, 700);

				for( String paragraph: paragraphs ) {
					for(String line: paragraph.split("\n")) {
						contents.showText( line );
						contents.newLineAtOffset( 0, -15);
					}
					contents.newLineAtOffset( 0, -15);
				}

				contents.endText();
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save( baos );
			return baos.toByteArray();
		}
		catch (IOException exc) {
			throw new PnInternalException("Generating PDF", exc);
		}
	}


	private TimelineElement timelineElement(Action action) {
		Optional<TimelineElement> row;
        row = this.timelineDao.getTimelineElement( action.getIun(), action.getActionId() );
        if ( !row.isPresent() ) {
        	String msg = "Error while retrieving timeline for IUN %s and action %s";
        	msg = String.format( msg, action.getIun(), action.getActionId() );
        	log.debug( msg );
        	throw new PnInternalException( msg );
        }
		return row.get();
	}
	    
    public String instantToDate(Instant instant) {
    	String suffix;
        Instant nextTransition = ROME_ZONE.getRules().nextTransition( instant ).getInstant();
        boolean isAmbiguous = isNear( instant, nextTransition );
        
        if( ! isAmbiguous ) {
            Instant prevTransition = ROME_ZONE.getRules().previousTransition( instant ).getInstant();
            isAmbiguous = isNear( instant, prevTransition );
            if( isAmbiguous ) {
                suffix = " CET";
            }
            else {
                suffix = "";
            }
        }
        else {
            suffix = " CEST";
        }
        
        LocalDateTime localDate = LocalDateTime.ofInstant(instant, ROME_ZONE);
        String date = localDate.format( ITALIAN_DATE_TIME_FORMAT );
        
        return date + suffix;
    }
    
    private boolean isNear( Instant a, Instant b) {
        Instant min;
        Instant max;
        if( a.isBefore(b) ) {
            min = a;
            max = b;
        }
        else {
            min = b;
            max = a;
        }
        Duration timeInterval = Duration.between(min, max);
        return ONE_HOUR.compareTo(timeInterval) >= 0;
    }
    
	protected String nullSafePhysicalAddressToString( NotificationRecipient recipient ) {
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

