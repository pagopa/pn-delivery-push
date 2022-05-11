package it.pagopa.pn.deliverypush.legalfacts;


import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.DateUtils;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendDigitalFeedback;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@ConditionalOnProperty(name = "pn.legalfacts.generator", havingValue = "OPENHTML2PDF")
public class OpenhtmltopdfLegalFactPdfGenerator extends AbstractLegalFactPdfGenerator implements LegalFactPdfGenerator {

    private static final String PARAGRAPH1 = "Ai sensi dell’art. 26, comma 11, del decreto-legge,"
            + " la PagoPA s.p.a. nella sua qualità di gestore ex lege"
            + " ella Piattaforma Notifiche Digitali di cui allo stesso art. 26,"
            + " con ogni valore legale per l'opponibilità a terzi, ATTESTA CHE:";

    private static final String HTML_TEMPLATE = "<html>"
            + "<head>"
            + "<style type=\"text/css\" media=\"all\">"
            + ".container {"
            + "width: 625px;"
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
    public static final AtomicReference<String> RESOURCES_BASE_URI = new AtomicReference<>();

    @Autowired
    public OpenhtmltopdfLegalFactPdfGenerator(TimelineDao timelineDao) {
        super(timelineDao);
    }

    @Override
    public byte[] generateNotificationReceivedLegalFact(Action action, NotificationInt notification) {
        String paragraph2 = DIV_PARAGRAPH
                + "in data %s il soggetto mittente <i>Denominazione IPA della PA con ID:</i> %s,"
                + " <i>Codice Fiscale della PA con ID:</i> %s "
                + "ha messo a disposizione del gestore i documenti informatici di "
                + "cui allo IUN %s e identificati in modo univoco con i seguenti hash: ";

        final String paId = notification.getSender().getPaId();
        paragraph2 = String.format( paragraph2, this.instantToDate( notification.getSentAt() ),
                paId,
                paId,
                action.getIun());

        StringBuilder bld = hashUnorderedList(notification);

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

        for ( NotificationRecipientInt recipient : notification.getRecipients() ) {
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
            sb.append(String.format(SPAN_CLASS_VALUE, nullSafePhysicalAddressToString(recipient, "<br/>")));
            sb.append("</p>");
            sb.append("</div>");

            paragraphs.add( sb.toString() );
        }

        return toPdfBytes( paragraphs );
    }
    
    @Override
    public byte[] generateNotificationReceivedLegalFact(NotificationInt notification) {
        String paragraph2 = DIV_PARAGRAPH
                + "in data %s il soggetto mittente <i>Denominazione IPA della PA con ID:</i> %s,"
                + " <i>Codice Fiscale della PA con ID:</i> %s "
                + "ha messo a disposizione del gestore i documenti informatici di "
                + "cui allo IUN %s e identificati in modo univoco con i seguenti hash: ";

        final String paId = notification.getSender().getPaId();
        paragraph2 = String.format(paragraph2, this.instantToDate(notification.getSentAt()),
                paId,
                paId,
                notification.getIun());

        StringBuilder bld = hashUnorderedList(notification);

        paragraph2 += bld.toString();
        paragraph2 += "</div>";

        String paragraph3 = DIV_PARAGRAPH
                + "il soggetto mittente ha richiesto che la notificazione di tali documenti fosse eseguita nei "
                + "confronti dei seguenti soggetti destinatari che in seguito alle verifiche di cui all’art. 7, commi "
                + "1 e 2, del DPCM del - ........, sono indicati unitamente al loro domicilio digitale o in assenza al "
                + "loro indirizzo fisico utile ai fini della notificazione richiesta:"
                + "</div>";

        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(PARAGRAPH1);
        paragraphs.add(paragraph2);
        paragraphs.add(paragraph3);

        for (NotificationRecipientInt recipient : notification.getRecipients()) {
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
            sb.append(String.format(SPAN_CLASS_VALUE, nullSafePhysicalAddressToString(recipient, "<br/>")));
            sb.append("</p>");
            sb.append("</div>");

            paragraphs.add(sb.toString());
        }

        return toPdfBytes(paragraphs);
    }

    @Override
    public byte[] generateNotificationViewedLegalFact(Action action, NotificationInt notification) {
        if (action.getRecipientIndex() == null) {
            String msg = "Error while retrieving RecipientIndex for IUN %s";
            msg = String.format(msg, action.getIun());
            log.debug(msg);
            throw new PnInternalException(msg);
        }

        NotificationRecipientInt recipient = notification.getRecipients().get(action.getRecipientIndex());
        TimelineElementInternal row = timelineElement(action);

        String paragraph2 = DIV_PARAGRAPH
                + "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:</div>";
        paragraph2 = String.format(paragraph2, notification.getIun());

        final DigitalAddress digitalDomicile = recipient.getDigitalDomicile();
        String paragraph3;
        if (digitalDomicile != null) {
            paragraph3 = DIV_PARAGRAPH
                    + "nome e cognome/ragione sociale %s, C.F. %s "
                    + "domicilio digitale %s: in data %s il destinatario ha avuto "
                    + "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato."
                    + "</div>";
            paragraph3 = String.format(paragraph3, recipient.getDenomination(),
                    recipient.getTaxId(),
                    digitalDomicile.getAddress(),
                    row.getTimestamp());
        } else {
            paragraph3 = DIV_PARAGRAPH
                    + "nome e cognome/ragione sociale %s, C.F. %s "
                    + "domicilio analogico %s: in data %s il destinatario ha avuto "
                    + "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato."
                    + "</div>";
            paragraph3 = String.format(paragraph3, recipient.getDenomination(),
                    recipient.getTaxId(),
                    recipient.getPhysicalAddress().getAddress(),
                    row.getTimestamp());
        }

        String paragraph4 = DIV_PARAGRAPH
                + "Si segnala che ogni successivo accesso ai medesimi documenti non è oggetto della presente "
                + "attestazione in quanto irrilevante ai fini del perfezionamento della notificazione."
                + "</div>";

        return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3, paragraph4));
    }

    @Override
    public byte[] generateNotificationViewedLegalFact(String iun, NotificationRecipientInt recipient, Instant timeStamp) {
            String paragraph2 = DIV_PARAGRAPH
                    + "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:</div>";
            paragraph2 = String.format(paragraph2, iun);

            final DigitalAddress digitalDomicile = recipient.getDigitalDomicile();
            String paragraph3;
            if (digitalDomicile != null) {
                paragraph3 = DIV_PARAGRAPH
                        + "nome e cognome/ragione sociale %s, C.F. %s "
                        + "domicilio digitale %s: in data %s il destinatario ha avuto "
                        + "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato."
                        + "</div>";
                paragraph3 = String.format(paragraph3, recipient.getDenomination(),
                        recipient.getTaxId(),
                        digitalDomicile.getAddress(),
                        this.instantToDate(timeStamp));
            } else {
                paragraph3 = DIV_PARAGRAPH
                        + "nome e cognome/ragione sociale %s, C.F. %s "
                        + "domicilio analogico %s: in data %s il destinatario ha avuto "
                        + "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato."
                        + "</div>";
                paragraph3 = String.format(paragraph3, recipient.getDenomination(),
                        recipient.getTaxId(),
                        recipient.getPhysicalAddress().getAddress(),
                        this.instantToDate(timeStamp));
            }

            String paragraph4 = DIV_PARAGRAPH
                    + "Si segnala che ogni successivo accesso ai medesimi documenti non è oggetto della presente "
                    + "attestazione in quanto irrilevante ai fini del perfezionamento della notificazione."
                    + "</div>";

            return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3, paragraph4));
        
    }
    
    /*
    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, NotificationInt notification, NotificationPathChooseDetails addresses) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(PARAGRAPH1);
        String paragraph2 = String.format(
                DIV_PARAGRAPH + "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:</div> <ul>",
                notification.getIun()
        );

        StringBuilder paragraph3 = new StringBuilder();
        for (Action action : actions) {
            //TODO Capire bene cosa vada valorizzato qui
            DigitalAddress address = action.getDigitalAddressSource().getAddressFrom(addresses);
            NotificationRecipientInt recipient = notification.getRecipients().get(action.getRecipientIndex());
            PnExtChnProgressStatus status = action.getResponseStatus();

            paragraph3.append(String.format(
                    DIV_PARAGRAPH + "<li> nome e cognome/ragione sociale %s, C.F. %s con domicilio digitale %s: ",
                    recipient.getDenomination(),
                    recipient.getTaxId(),
                    address.getAddress()
            ));

            TimelineElementInternal  row = timelineElement(action);
            Instant timestamp = row.getTimestamp();

            if (PnExtChnProgressStatus.OK.equals(status)) {
                paragraph3.append(String.format(
                        "il relativo avviso di avvenuta ricezione in formato elettronico è stato consegnato in data %s </li>",
                        this.instantToDate(timestamp)
                ));
            } else {
                paragraph3.append(String.format(
                        "in data %s è stato ricevuto il relativo messaggio di mancato recapito al domicilio digitale già indicato.</li>",
                        this.instantToDate(timestamp)
                ));
            }
            paragraph3.append("</div>");
        }
        paragraph3.append("</ul>");

        return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3.toString()));
    }*/

    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> listFeedbackFromExtChannel, NotificationInt notification, NotificationRecipientInt recipient) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(PARAGRAPH1);
        String paragraph2 = String.format(
                DIV_PARAGRAPH + "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:</div> <ul>",
                notification.getIun()
        );

        StringBuilder paragraph3 = new StringBuilder();
        for (SendDigitalFeedback digitalFeedback : listFeedbackFromExtChannel) {
            DigitalAddress address = digitalFeedback.getAddress();
            ResponseStatus status = digitalFeedback.getResponseStatus();

            paragraph3.append(String.format(
                    DIV_PARAGRAPH + "<li> nome e cognome/ragione sociale %s, C.F. %s con domicilio digitale %s: ",
                    recipient.getDenomination(),
                    recipient.getTaxId(),
                    address.getAddress()
            ));

            Instant timestamp = DateUtils.convertDateToInstant(digitalFeedback.getNotificationDate());

            if (ResponseStatus.OK.equals(status)) {
                paragraph3.append(String.format(
                        "il relativo avviso di avvenuta ricezione in formato elettronico è stato consegnato in data %s </li>",
                        this.instantToDate(timestamp)
                ));
            } else {
                paragraph3.append(String.format(
                        "in data %s è stato ricevuto il relativo messaggio di mancato recapito al domicilio digitale già indicato.</li>",
                        this.instantToDate(timestamp)
                ));
            }
            paragraph3.append("</div>");
        }
        paragraph3.append("</ul>");

        return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3.toString()));
    }

    private byte[] toPdfBytes(List<String> paragraphs) throws PnInternalException {
        try {
            StringBuilder html = new StringBuilder();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();

            String fileUri = getResourceBaseUri();
            String baseUri = fileUri.replaceFirst("/[^/]*$", "/");

            html.append(HTML_TEMPLATE);
            for (int i = 0; i < paragraphs.size(); i++) {
                html.append(paragraphs.get(i));
            }
            html.append("</div></body></html>");

            builder.withHtmlContent(html.toString(), baseUri);
            builder.toStream(baos);
            builder.run();
            baos.close();

            return baos.toByteArray();
        } catch (IOException exc) {
            throw new PnInternalException("Error while generatin legalfact document", exc);
        }
    }

    private String getResourceBaseUri() {
        String baseUri = RESOURCES_BASE_URI.get();
        if (baseUri == null) {
            try {
                baseUri = Thread.currentThread().getContextClassLoader().getResource("image/pn-logo-footer.png").toString();
                RESOURCES_BASE_URI.set(baseUri);
            } catch (RuntimeException exc) {
                // In case of error ignore image
                baseUri = "ignore";
            }
        }

        return baseUri;
    }

    protected StringBuilder hashUnorderedList(NotificationInt notification) {
        StringBuilder bld = new StringBuilder();
        bld.append("<ul>");
        for (int idx = 0; idx < notification.getDocuments().size(); idx++) {
            bld.append(unorderedListItem(notification.getDocuments().get(idx).getDigests().getSha256()));
        }

        //TODO Verificare è corretto che qui vengano inserite le informazioni di pagamento di tutti i recipient
        notification.getRecipients().forEach(
                recipient -> {
                    if (recipient.getPayment() != null) {
                        if (recipient.getPayment().getF24flatRate() != null) {
                            bld.append(unorderedListItem(recipient.getPayment().getF24flatRate().getDigests().getSha256()));
                        }
                        if (recipient.getPayment().getF24white() != null) {
                            bld.append(unorderedListItem(recipient.getPayment().getF24white().getDigests().getSha256()));
                        }
                        if (recipient.getPayment().getPagoPaForm() != null) {
                            bld.append(unorderedListItem(recipient.getPayment().getPagoPaForm().getDigests().getSha256()));
                        }
                    }
                }
        );
        
        
        bld.append("</ul>");

        return bld;
    }

    private String unorderedListItem(String sha256) {
        return "<li>" + sha256 + ";</li>";
    }
}

