package it.pagopa.pn.deliverypush.actions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

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
public class LegalFactPdfFromHtmlGeneratorUtils {
	
	private static final DateTimeFormatter ITALIAN_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final Duration ONE_HOUR = Duration.ofHours(1);
	private static final ZoneId ROME_ZONE = ZoneId.of("Europe/Rome");
    
	private final TimelineDao timelineDao;
	
	private static final String PARAGRAPH1 = "Ai sensi dell’art. 26, comma 11, del decreto-legge,"
			+ " la PagoPA s.p.a. nella sua qualità di gestore ex lege"
			+ " ella Piattaforma Notifiche Digitali di cui allo stesso art. 26,"
			+ " con ogni valore legale per l'opponibilità a terzi, ATTESTA CHE:";
	
	private static final String HTML_TEMPLATE = "<html>"
			+ "<head>"
			+ "<style type=\"text/css\" media=\"all\">"
			+ ".container {"
			+ "width: 635px;"
			+ "margin: 0 auto;"
			+ "margin-top: 25px;"
			+ "}"
			+ ".paragraph {"
			+ "font-family: \"Times New Roman\", Times, serif;"
			+ "font-size: 16px;"
			+ "margin-top: 25px;"
			+ "}"
			+ ".recipient {"
			+ "margin-top: 25px;"
			+ "page-break-inside: avoid !important;"
			+ "display: table;"
			+ "}"
			+ ".recipient_row {"
			+ "display: table-row;"
			+ "}"
			+ ".recipient_row > .title {"
			+ "display: table-cell;"
			+ "padding: 2pt;"
			+ "font-weight: bold;"
			+ "}"
			+ ".recipient_row > .value {"
			+ "display: table-cell;"
			+ "padding: 2pt;"
			+ "}"
			+ "p {"
			+ "margin: 1px;"
			+ "padding: 1px;"
			+ "}"
			+ "hr {"
			+ "border: 0;"
			+ "height: 1px;"
			+ "background: #888888;"
			+ "margin-bottom: 12px;"
			+ "}"
			+ ".footer {"
			+ "margin-top: 25px;"
			+ "margin-bottom: 25px;"
			+ "font-family: \"Times New Roman\", Times, serif;"
			+ "color: #888888;"
			+ "font-size: 12px;"
			+ "}"
			+ ".footer img {"
			+ "float: right;    "
			+ "margin: 0 0 0 15px;"
			+ "}"
			+ "#header {"
			+ "position: running(header);"
			+ "}"
			+ "#footer {"
			+ "margin-top: 25px;"
			+ "position: running(footer);"
			+ "}"
			+ "@page {"
			+ "size: A4;"
			+ "margin: 10%;"
			+ "margin-bottom: 150px;"
			+ "@top-left {"
			+ "content: element(header);"
			+ "}"
			+ "@bottom-left {"
			+ "content: element(footer);"
			+ "}"
			+ "}"
			+ "</style>"
			+ "</head>"
			+ "<body>"
			+ "<div class=\"container\">"
			+ "<div id=\"header\">"
			+ "<img src=\"pn-logo-header.png\" width=\"180\" height=\"55\" />"
			+ "</div>"
			+ "<div class=\"footer\" id=\"footer\">"
			+ "<hr/>"
			+ "<img id=\"footer_img\" src=\"pn-logo-footer.png\" width=\"70\" height=\"70\" />"
			+ "PagoPA S.p.A.<br/>"
			+ "società per azioni con socio unico<br/>"
			+ "capitale sociale di euro 1,000,000 interamente versato<br/>"
			+ "sede legale in Roma, Piazza Colonna 370, CAP 00187<br/>"
			+ "n. di iscrizione a Registro Imprese di Roma, CF e P.IVA 15376371009"
			+ "</div>";
	    
	private static final String DIV_PARAGRAPH = "<div class=\"paragraph\">";
	private static final String P_RECIPIENT_ROW = "<p class=\"recipient_row\">";
	private static final String SPAN_CLASS_VALUE = "<span class=\"value\">%s</span>";
			
	@Autowired
	public LegalFactPdfFromHtmlGeneratorUtils( TimelineDao timelineDao ) {
        this.timelineDao = timelineDao;
    }
    
	public byte[] generateNotificationReceivedLegalFact(Action action, Notification notification) {
		String paragraph2 = DIV_PARAGRAPH
				+ "in data %s il soggetto mittente %s, C.F. "
	    		+ "%s ha messo a disposizione del gestore i documenti informatici di "
	    		+ "cui allo IUN %s e identificati in modo univoco con i seguenti hash: ";
		
	    paragraph2 = String.format( paragraph2, this.instantToDate( notification.getSentAt() ),
	    										notification.getSender().getPaDenomination(),
	    										notification.getSender().getTaxId( notification.getSender().getPaId() ),
	    										action.getIun());
	    StringBuilder bld = new StringBuilder();
	    bld.append("<ul>");
	    for (int idx = 0; idx < notification.getDocuments().size(); idx ++) {
	    	bld.append(orderedItem(notification.getDocuments().get(idx).getDigests().getSha256()));
	    }
	    
	    if ( notification.getPayment() != null && notification.getPayment().getF24() != null ) {
	    	if ( notification.getPayment().getF24().getFlatRate() != null) {
	    		bld.append(orderedItem(notification.getPayment().getF24().getFlatRate().getDigests().getSha256()));
	    	}
	    	if ( notification.getPayment().getF24().getDigital() != null) {
	    		bld.append(orderedItem(notification.getPayment().getF24().getDigital().getDigests().getSha256()));
	    	}
	    	if ( notification.getPayment().getF24().getAnalog() != null) {
	    		bld.append(orderedItem(notification.getPayment().getF24().getAnalog().getDigests().getSha256()));
	    	}
	    }
	    bld.append("</ul>");
	    
	    paragraph2 += bld.toString();
	    paragraph2 += "</div>";
	    
	    String paragraph3 = DIV_PARAGRAPH
	    		+ "il soggetto mittente ha richiesto che la notificazione di tali documenti fosse eseguita nei "
	    		+ "confronti dei seguenti soggetti destinatari che in seguito alle verifiche di cui all’art. 7, commi "
	    		+ "1 e 2, del DPCM del - ........, sono indicati unitamente al loro domicilio digitale o in assenza al "
	    		+ "loro indirizzo fisico utile ai fini della notificazione richiesta:"
	    		+ "</div>";

		List<String> paragraphs = new ArrayList<>();
		paragraphs.add( PARAGRAPH1 );
		paragraphs.add( paragraph2 );
		paragraphs.add( paragraph3 );

		for ( NotificationRecipient recipient : notification.getRecipients() ) {
			final DigitalAddress digitalDomicile = recipient.getDigitalDomicile();

			StringBuilder sb = new StringBuilder();
			sb.append("<div class=\"recipient\">");
			sb.append(P_RECIPIENT_ROW);
			sb.append("<span class=\"title\">nome e cognome/ragione sociale</span>");
			sb.append(String.format(SPAN_CLASS_VALUE, recipient.getDenomination()));
			sb.append("</p>");
			sb.append(P_RECIPIENT_ROW);
			sb.append("<span class=\"title\">Codice Fiscale</span>");
			sb.append(String.format(SPAN_CLASS_VALUE, recipient.getTaxId()));
			sb.append("</p>");
			sb.append(P_RECIPIENT_ROW);
			sb.append("<span class=\"title\">Domicilio Digitale</span>");
			sb.append(String.format(SPAN_CLASS_VALUE, digitalDomicile != null ? digitalDomicile.getAddress() : ""));
			sb.append("</p>");
			sb.append(P_RECIPIENT_ROW);
			sb.append("<span class=\"title\">Indirizzo Fisico</span>");
			sb.append(String.format(SPAN_CLASS_VALUE, nullSafePhysicalAddressToString(recipient)));
			sb.append("</p>");
			sb.append("</div>");
			
			paragraphs.add( sb.toString() );
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
        
	    String paragraph2 = DIV_PARAGRAPH
	    		+ "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:</div>";
	    paragraph2 = String.format( paragraph2, notification.getIun() );
	    
	    String paragraph3 = DIV_PARAGRAPH
	    		+ "nome e cognome/ragione sociale %s, C.F. %s "
	    		+ "domicilio digitale %s: in data %s il destinatario ha avuto "
	    		+ "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato."
	    		+ "</div>";
	    paragraph3 = String.format( paragraph3, recipient.getDenomination(), 
	    										recipient.getTaxId(), 
	    										recipient.getDigitalDomicile().getAddress(), 
	    										this.instantToDate( row.getTimestamp() ) );
	    
	    String paragraph4 = DIV_PARAGRAPH
	    		+ "Si segnala che ogni successivo accesso ai medesimi documenti non è oggetto della presente "
	    		+ "attestazione in quanto irrilevante ai fini del perfezionamento della notificazione."
	    		+ "</div>";

		return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3, paragraph4));
	}
	
	public byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses) {
		List<String> paragraphs = new ArrayList<>();
		paragraphs.add( PARAGRAPH1 );
		String paragraph2 = String.format(
				DIV_PARAGRAPH + "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:</div>",
				notification.getIun()
			);

		StringBuilder paragraph3 = new StringBuilder();
    	for (Action action : actions) {
	    	DigitalAddress address = action.getDigitalAddressSource().getAddressFrom( addresses );
	    	NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
	    	PnExtChnProgressStatus status = action.getResponseStatus();
	       	    	
	    	paragraph3.append( String.format(
	    			DIV_PARAGRAPH + "nome e cognome/ragione sociale %s, C.F. %s con domicilio digitale %s: ",
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
	    	paragraph3.append("</div>");
		}

		return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3.toString()));
	}

	private byte[] toPdfBytes( List<String> paragraphs) throws PnInternalException {
        try {
        	StringBuilder html = new StringBuilder();
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		PdfRendererBuilder builder = new PdfRendererBuilder();
    		
    		Path resourceDirectory = Paths.get("src", "main", "resources", "image");
    		String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    		String baseUri = FileSystems.getDefault().getPath(absolutePath).toUri().toString();
    		
    		html.append(HTML_TEMPLATE);
    		for (int i=0; i<paragraphs.size(); i++) {
    			html.append( paragraphs.get(i) );
    		}
    		html.append("</div></body></html>");
    		
    		builder.withHtmlContent(html.toString(), baseUri);
    		builder.toStream(baos);
    		builder.run();
    		baos.close();

    		return baos.toByteArray();
        }
        catch (IOException exc) {
        	throw new PnInternalException("Error while generatin legalfact document", exc);
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
					result = String.join("<br/>", standardAddressString );
				}
			}
		}
	
		return result;
	}
	
	private String orderedItem(String sha256) {
		return "<li>" + sha256 + ";</li>";
	}
}

