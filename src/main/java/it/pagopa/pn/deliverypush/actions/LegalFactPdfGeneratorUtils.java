package it.pagopa.pn.deliverypush.actions;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.List;

import org.springframework.stereotype.Component;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;

@Component
public class LegalFactPdfGeneratorUtils {
	
	private static final String PARAGRAPH1 = "Ai sensi dell’art. 26, comma 11, del decreto-legge, la PagoPA s.p.a. nella sua qualità di\n"
			+ "gestore ex lege della Piattaforma Notifiche Digitali di cui allo stesso art. 26, con ogni valore\n"
			+ "legale per l'opponibilità a terzi, ATTESTA CHE:";
    
	public byte[] generateNotificationReceivedLegalFact(Action action, Notification notification, NotificationRecipient recipient) throws DocumentException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Document document = new Document();
    	PdfWriter writer = PdfWriter.getInstance( document, baos );
			
	    document.open();
	    	    	    
	    String paragraph2 = "in data %s il soggetto mittente %s, C.F. "
	    		+ "%s ha messo a disposizione del gestore i documenti informatici di "
	    		+ "cui allo IUN %s e identificati in modo univoco con i seguenti hash:\n";
	    paragraph2 = String.format( paragraph2, this.instantToDate(Instant.now()),
	    										notification.getSender().getPaDenomination(),
	    										notification.getSender().getPaId(),
	    										action.getIun());
	    StringBuilder bld = new StringBuilder();
	    for (int idx = 0; idx < notification.getDocuments().size(); idx ++) {
	    	bld.append( notification.getDocuments().get(idx).getDigests().getSha256() + ";\n" );
	    }
	    paragraph2 = paragraph2 + bld.toString();
	    
	    String paragraph3 = "il soggetto mittente ha richiesto che la notificazione di tali documenti fosse eseguita nei\n"
	    		+ "confronti dei seguenti soggetti destinatari che in seguito alle verifiche di cui all’art. 7, commi\n"
	    		+ "1 e 2, del DPCM del -........, sono indicati unitamente al loro domicilio digitale o in assenza al\n"
	    		+ "loro indirizzo fisico utile ai fini della notificazione richiesta:";
	    
	    String paragraph4 = "nome e cognome/ragione sociale %s, C.F. %s\n"
	    		+ "domicilio digitale %s, indirizzo fisico %s;";
	    paragraph4 = String.format( paragraph4, recipient.getDenomination(),
												recipient.getTaxId(),
												recipient.getDigitalDomicile().getAddress(),
												nullSafePhysicalAddressToString( recipient ) );
	    
	    document.add( new Paragraph( PARAGRAPH1 ) );
	    document.add( Chunk.NEWLINE );
	    document.add( new Paragraph( paragraph2 ) );
	    document.add( Chunk.NEWLINE );
	    document.add( new Paragraph( paragraph3 ) );
	    document.add( Chunk.NEWLINE );
	    document.add( new Paragraph( paragraph4 ) );
	    
	    document.close();
		writer.close();
		
		return baos.toByteArray();
	}
	
	public byte[] generateNotificationViewedLegalFact(Action action, Notification notification) throws DocumentException {
		NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Document document = new Document();
    	PdfWriter writer = PdfWriter.getInstance( document, baos );
			
	    document.open();
	    	
	    String paragraph2 = "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:";
	    paragraph2 = String.format( paragraph2, notification.getIun() );
	    
	    String paragraph3 = "nome e cognome/ragione sociale %s, C.F. %s\n"
	    		+ "domicilio digitale %s: in data %s il destinatario ha avuto "
	    		+ "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato.";
	    paragraph3 = String.format( paragraph3, recipient.getDenomination(), 
	    										recipient.getTaxId(), 
	    										recipient.getDigitalDomicile().getAddress(), 
	    										this.instantToDate( Instant.now() ) );
	    
	    String paragraph4 = "Si segnala che ogni successivo accesso ai medesimi documenti non è oggetto della presente\n"
	    		+ "attestazione in quanto irrilevante ai fini del perfezionamento della notificazione.";
	    
	    document.add( new Paragraph( PARAGRAPH1 ) );
	    document.add( Chunk.NEWLINE );
	    document.add( new Paragraph( paragraph2 ) );
	    document.add( Chunk.NEWLINE );
	    document.add( new Paragraph( paragraph3 ) );
	    document.add( Chunk.NEWLINE );
	    document.add( new Paragraph( paragraph4 ) );
	    
		document.close();
		writer.close();
		
		return baos.toByteArray();
	}
	
	public byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses) throws DocumentException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Document document = new Document();
    	PdfWriter writer = PdfWriter.getInstance( document, baos );
    	
	    document.open();
	    
	    for (Action action : actions) {
	    	DigitalAddress address = action.getDigitalAddressSource().getAddressFrom( addresses );
	    	NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
	    	PnExtChnProgressStatus status = action.getResponseStatus();
	       	    	
	    	String paragraph2 = "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:";
	    	paragraph2 = String.format( paragraph2, notification.getIun() );
	    	
	    	String paragraph3 = "nome e cognome/ragione sociale %s, C.F. %s con "
	    			+ "domicilio digitale %s: il relativo avviso di avvenuta ricezione in "
	    			+ "formato elettronico è stato consegnato in data %s / in data %s è "
	    			+ "stato ricevuto il relativo messaggio di mancato recapito al domicilio digitale già indicato.";
	    	paragraph3 = String.format( paragraph3, recipient.getDenomination(),
	    											recipient.getTaxId(),
	    											address.getAddress(),
	    											PnExtChnProgressStatus.OK.equals( status ) ? this.instantToDate( Instant.now() ) : "",
	    											PnExtChnProgressStatus.OK.equals( status ) ? "" : this.instantToDate( Instant.now() )
	    											);
	    	
	    	document.add( new Paragraph( PARAGRAPH1 ) );
		    document.add( Chunk.NEWLINE );
		    document.add( new Paragraph( paragraph2 ) );
		    document.add( Chunk.NEWLINE );
		    document.add( new Paragraph( paragraph3 ) );
		    document.add( Chunk.NEWLINE );
	    }
	    
	    document.close();
		writer.close();
		
		return baos.toByteArray();
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
