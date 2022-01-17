package it.pagopa.pn.deliverypush.legalfacts;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedback;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(name = "pn.legalfacts.generator", havingValue = "PDFBOX")
public class PdfboxLegalFactPdfGenerator extends AbstractLegalFactPdfGenerator implements LegalFactPdfGenerator {

    private static final String PARAGRAPH1 = "Ai sensi dell’art. 26, comma 11, del decreto-legge,"
            + " la PagoPA s.p.a. nella sua qualità di gestore ex lege"
            + " ella Piattaforma Notifiche Digitali di cui allo stesso art. 26,"
            + " con ogni valore legale per l'opponibilità a terzi, ATTESTA CHE:";

    private static final PDFont FONT = PDType1Font.TIMES_ROMAN;
    private static final float FONT_SIZE = 12;
    private static final float LEADING = -1.5f * FONT_SIZE;
    private static final float TY_NEW_PAGE = 186f;

    private static final float MARGINY = 120f;
    private static final float MARGINX = 80f;

    private PDDocument document = null;
    private PDPageContentStream contentStream = null;
    private float calculatedTY;

    private float startX;
    private float startY;

    @Autowired
    public PdfboxLegalFactPdfGenerator(TimelineDao timelineDao) {
        super(timelineDao);
    }

    @Override
    public byte[] generateNotificationReceivedLegalFact(Notification notification) {
        String paragraph2 = "in data %s il soggetto mittente %s, C.F. "
                + "%s ha messo a disposizione del gestore i documenti informatici di "
                + "cui allo IUN %s e identificati in modo univoco con i seguenti hash: ";
        paragraph2 = String.format(paragraph2, this.instantToDate(notification.getSentAt()),
                notification.getSender().getPaDenomination(),
                notification.getSender().getTaxId(notification.getSender().getPaId()),
                notification.getIun());
        StringBuilder bld = new StringBuilder();
        for (int idx = 0; idx < notification.getDocuments().size(); idx++) {
            bld.append("\n" + notification.getDocuments().get(idx).getDigests().getSha256() + "; ");
        }

        if (notification.getPayment() != null && notification.getPayment().getF24() != null) {
            if (notification.getPayment().getF24().getFlatRate() != null) {
                bld.append("\n" + notification.getPayment().getF24().getFlatRate().getDigests().getSha256() + ";");
            }
            if (notification.getPayment().getF24().getDigital() != null) {
                bld.append("\n" + notification.getPayment().getF24().getDigital().getDigests().getSha256() + ";");
            }
            if (notification.getPayment().getF24().getAnalog() != null) {
                bld.append("\n" + notification.getPayment().getF24().getAnalog().getDigests().getSha256() + ";");
            }
        }

        paragraph2 = paragraph2 + bld.toString();

        String paragraph3 = "il soggetto mittente ha richiesto che la notificazione di tali documenti fosse eseguita nei "
                + "confronti dei seguenti soggetti destinatari che in seguito alle verifiche di cui all’art. 7, commi "
                + "1 e 2, del DPCM del - ........, sono indicati unitamente al loro domicilio digitale o in assenza al "
                + "loro indirizzo fisico utile ai fini della notificazione richiesta:";

        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(PARAGRAPH1);
        paragraphs.add(paragraph2);
        paragraphs.add(paragraph3);

        for (NotificationRecipient recipient : notification.getRecipients()) {
            final DigitalAddress digitalDomicile = recipient.getDigitalDomicile();
            paragraphs.add(String.format(
                    "nome e cognome/ragione sociale %s, C.F. %s domicilio digitale %s, indirizzo fisico %s;",
                    recipient.getDenomination(),
                    recipient.getTaxId(),
                    digitalDomicile != null ? digitalDomicile.getAddress() : "",
                    "\n" + nullSafePhysicalAddressToString(recipient, "\n")
            ));
        }

        return toPdfBytes(paragraphs);
    }

    @Override
    public byte[] generateNotificationViewedLegalFact(Action action, Notification notification) {
        if (action.getRecipientIndex() == null) {
            String msg = "Error while retrieving RecipientIndex for IUN %s";
            msg = String.format(msg, action.getIun());
            log.debug(msg);
            throw new PnInternalException(msg);
        }

        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
        TimelineElement row = timelineElement(action);

        String paragraph2 = "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:";
        paragraph2 = String.format(paragraph2, notification.getIun());

        String paragraph3 = "nome e cognome/ragione sociale %s, C.F. %s "
                + "domicilio digitale %s: in data %s il destinatario ha avuto "
                + "accesso ai documenti informatici oggetto di notifica e associati allo IUN già indicato.";
        paragraph3 = String.format(paragraph3, recipient.getDenomination(),
                recipient.getTaxId(),
                recipient.getDigitalDomicile().getAddress(),
                this.instantToDate(row.getTimestamp()));

        String paragraph4 = "Si segnala che ogni successivo accesso ai medesimi documenti non è oggetto della presente "
                + "attestazione in quanto irrilevante ai fini del perfezionamento della notificazione.";

        return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3, paragraph4));
    }

    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<Action> actions, Notification notification, NotificationPathChooseDetails addresses) {

        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(PARAGRAPH1);
        String paragraph2 = String.format(
                "gli atti di cui alla notifica identificata con IUN %s sono stati gestiti come segue:",
                notification.getIun()
        );

        StringBuilder paragraph3 = new StringBuilder();
        for (Action action : actions) {
            DigitalAddress address = action.getDigitalAddressSource().getAddressFrom(addresses);
            NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
            PnExtChnProgressStatus status = action.getResponseStatus();

            paragraph3.append(String.format(
                    "nome e cognome/ragione sociale %s, C.F. %s con domicilio digitale %s: ",
                    recipient.getDenomination(),
                    recipient.getTaxId(),
                    address.getAddress()
            ));

            TimelineElement row = timelineElement(action);
            Instant timestamp = row.getTimestamp();

            if (PnExtChnProgressStatus.OK.equals(status)) {
                paragraph3.append(String.format(
                        "il relativo avviso di avvenuta ricezione in formato elettronico è stato consegnato in data %s",
                        this.instantToDate(timestamp)
                ));
            } else {
                paragraph3.append(String.format(
                        "in data %s è stato ricevuto il relativo messaggio di mancato recapito al domicilio digitale già indicato.",
                        this.instantToDate(timestamp)
                ));
            }
        }

        return toPdfBytes(Arrays.asList(PARAGRAPH1, paragraph2, paragraph3.toString()));
    }

    @Override
    public byte[] generatePecDeliveryWorkflowLegalFact(List<SendDigitalFeedback> listFeedbackFromExtChannel, Notification notification, NotificationRecipient recipient) {
        throw new UnsupportedOperationException();
    }

    private byte[] toPdfBytes(List<String> paragraphs) throws PnInternalException {
        try {
            PDPage page = new PDPage();
            document = new PDDocument();
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);

            PDRectangle mediaBox = page.getMediaBox();
            startX = mediaBox.getLowerLeftX() + MARGINX;
            startY = mediaBox.getUpperRightY() - MARGINY;
            float width = mediaBox.getWidth() - 2 * MARGINX;
            calculatedTY = startY;

            printHeader(document, contentStream);
            contentStream.beginText();

            for (int i = 0; i < paragraphs.size(); i++) {
                if (i == 0) {
                    addParagraph(width, startX, startY, paragraphs.get(i));
                } else {
                    addParagraph(width, 0, -FONT_SIZE, paragraphs.get(i));
                }
            }

            contentStream.endText();
            printFooter(document, contentStream);
            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);

            if (!document.getDocument().isClosed()) {
                document.close();
            }

            return baos.toByteArray();
        } catch (IOException exc) {
            throw new PnInternalException("Error while generatin legalfact document", exc);
        }
    }

    private void addParagraph(float width, float sx, float sy, String text) throws IOException {
        addParagraph(width, sx, sy, text, false);
    }

    private void addParagraph(float width, float sx, float sy, String text, boolean justify) throws IOException {
        List<String> lines = parseLines(text, width);
        contentStream.setFont(FONT, FONT_SIZE);
        contentStream.newLineAtOffset(sx, sy);
        for (String line : lines) {

            if (calculatedTY <= TY_NEW_PAGE) {
                contentStream.endText();
                printFooter(document, contentStream);
                contentStream.close();

                PDPage newPage = new PDPage();
                document.addPage(newPage);
                contentStream = new PDPageContentStream(document, newPage);

                PDRectangle mediaBox = newPage.getMediaBox();
                startX = mediaBox.getLowerLeftX() + MARGINX;
                startY = mediaBox.getUpperRightY() - MARGINY;

                printHeader(document, contentStream);

                contentStream.beginText();
                contentStream.setFont(FONT, FONT_SIZE);
                contentStream.newLineAtOffset(startX, startY);
                calculatedTY = startY;
            }

            float charSpacing = 0;
            if (justify && line.length() > 1) {
                float size = FONT_SIZE * FONT.getStringWidth(line) / 1000;
                float free = width - size;
                if (free > 0 && !lines.get(lines.size() - 1).equals(line)) {
                    charSpacing = free / (line.length() - 1);
                }
            }
            contentStream.setCharacterSpacing(charSpacing);
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, LEADING);
            calculatedTY += LEADING;
        }
    }

    private List<String> parseLines(String text, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        int lastSpace = -1;
        for (String text1 : text.split("\n")) {
            while (text1.length() > 0) {
                int spaceIndex = text1.indexOf(' ', lastSpace + 1);
                if (spaceIndex < 0)
                    spaceIndex = text1.length();
                String subString = text1.substring(0, spaceIndex);
                float size = FONT_SIZE * FONT.getStringWidth(subString) / 1000;
                if (size > width) {
                    if (lastSpace < 0) {
                        lastSpace = spaceIndex;
                    }
                    subString = text1.substring(0, lastSpace);
                    lines.add(subString);
                    text1 = text1.substring(lastSpace).trim();
                    lastSpace = -1;
                } else if (spaceIndex == text1.length()) {
                    lines.add(text1);
                    text1 = "";
                } else {
                    lastSpace = spaceIndex;
                }
            }
        }
        return lines;
    }

    private void printHeader(final PDDocument document, PDPageContentStream contentStream) {
        try {
            File headerLogo = ResourceUtils.getFile("classpath:image/pn-logo-header.png");
            PDImageXObject ximage = PDImageXObject.createFromFileByContent(headerLogo, document);
            float scale = 0.22f;
            contentStream.drawImage(ximage, 80, 720, ximage.getWidth() * scale, ximage.getHeight() * scale);
        } catch (IOException e) {
            throw new PnInternalException("Error loading header image logo while generating legalfacts pdf document", e);
        }
    }

    private void printFooter(PDDocument document, PDPageContentStream cos) throws IOException {
        PDFont font = PDType1Font.TIMES_ROMAN;
        float ty = 120;
        ArrayList<String> footerText = new ArrayList<>(
                Arrays.asList("PagoPA S.p.A",
                        "società per azioni con socio unico",
                        "capitale sociale di euro 1,000,000 interamente versato",
                        "sede legale in Roma, Piazza Colonna 370, CAP 00187",
                        "n. di iscrizione a Registro Imprese di Roma, CF e P.IVA 15376371009"
                )
        );

        //print separator
        cos.setLineWidth(0.5f);
        cos.moveTo(80, 140);
        cos.lineTo(520, 140);
        cos.setStrokingColor(Color.DARK_GRAY);
        cos.closeAndStroke();

        //print text
        for (int i = 0; i < footerText.size(); i++) {
            cos.beginText();
            cos.setFont(font, 8);
            cos.setNonStrokingColor(Color.DARK_GRAY);
            cos.newLineAtOffset(80, ty);
            cos.showText(footerText.get(i));
            cos.endText();
            ty -= 15;
        }

        //print logo
        try {
            File footerLogo = ResourceUtils.getFile("classpath:image/pn-logo-footer.png");
            PDImageXObject ximage1 = PDImageXObject.createFromFileByContent(footerLogo, document);
            float scale = 0.25f;
            cos.drawImage(ximage1, 480, 80, ximage1.getWidth() * scale, ximage1.getHeight() * scale);
        } catch (IOException e) {
            throw new PnInternalException("Error loading footer image logo while generating legalfacts pdf document", e);
        }
    }

}

