package it.pagopa.pn.deliverypush.legalfacts;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.utils.HtmlSanitizer;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DOCUMENTCOMPOSITIONFAILED;

@Component
@Slf4j
public class DocumentComposition {

    public static TemplateType retrieveTemplateFromLang(TemplateType templateType, String additionalLang) {
        String finalTemplateName = templateType.name() + "_" + additionalLang;
        log.info("Retrieve template [{}] from lang: [{}]", finalTemplateName, additionalLang);
        return DocumentComposition.TemplateType.valueOf(finalTemplateName);
    }

    public enum TemplateType {
        REQUEST_ACCEPTED("documents_composition_templates/NotificationReceivedLegalFact.html"),
        REQUEST_ACCEPTED_DE("documents_composition_templates/NotificationReceivedLegalFact_de.html"),
        REQUEST_ACCEPTED_SL("documents_composition_templates/NotificationReceivedLegalFact_sl.html"),
        REQUEST_ACCEPTED_FR("documents_composition_templates/NotificationReceivedLegalFact_fr.html"),


        DIGITAL_NOTIFICATION_WORKFLOW("documents_composition_templates/PecDeliveryWorkflowLegalFact.html"),
        DIGITAL_NOTIFICATION_WORKFLOW_DE("documents_composition_templates/PecDeliveryWorkflowLegalFact_de.html"),
        DIGITAL_NOTIFICATION_WORKFLOW_SL("documents_composition_templates/PecDeliveryWorkflowLegalFact_sl.html"),
        DIGITAL_NOTIFICATION_WORKFLOW_FR("documents_composition_templates/PecDeliveryWorkflowLegalFact_fr.html"),


        ANALOG_NOTIFICATION_WORKFLOW_FAILURE("documents_composition_templates/AnalogDeliveryWorkflowFailureLegalFact.html"),
        ANALOG_NOTIFICATION_WORKFLOW_FAILURE_DE("documents_composition_templates/AnalogDeliveryWorkflowFailureLegalFact_de.html"),
        ANALOG_NOTIFICATION_WORKFLOW_FAILURE_SL("documents_composition_templates/AnalogDeliveryWorkflowFailureLegalFact_sl.html"),
        ANALOG_NOTIFICATION_WORKFLOW_FAILURE_FR("documents_composition_templates/AnalogDeliveryWorkflowFailureLegalFact_fr.html"),


        NOTIFICATION_VIEWED("documents_composition_templates/NotificationViewedLegalFact.html"),
        NOTIFICATION_VIEWED_DE("documents_composition_templates/NotificationViewedLegalFact_de.html"),
        NOTIFICATION_VIEWED_DE("documents_composition_templates/NotificationViewedLegalFact_de.html"),
        NOTIFICATION_VIEWED_SL("documents_composition_templates/NotificationViewedLegalFact_sl.html"),
        NOTIFICATION_VIEWED_FR("documents_composition_templates/NotificationViewedLegalFact_fr.html"),


        AAR_NOTIFICATION("documents_composition_templates/NotificationAAR.html"),
        AAR_NOTIFICATION_DE("documents_composition_templates/NotificationAAR_de.html"),
        AAR_NOTIFICATION_SL("documents_composition_templates/NotificationAAR_sl.html"),
        AAR_NOTIFICATION_FR("documents_composition_templates/NotificationAAR_fr.html"),


        AAR_NOTIFICATION_RADD("documents_composition_templates/NotificationAAR_RADD.html"),


        AAR_NOTIFICATION_RADD_ALT("documents_composition_templates/NotificationAAR_RADDalt_old.html"),
        AAR_NOTIFICATION_RADD_ALT_DE("documents_composition_templates/NotificationAAR_RADDalt_old_de.html"),
        AAR_NOTIFICATION_RADD_ALT_SL("documents_composition_templates/NotificationAAR_RADDalt_old_sl.html"),
        AAR_NOTIFICATION_RADD_ALT_FR("documents_composition_templates/NotificationAAR_RADDalt_old_fr.html"),


        AAR_NOTIFICATION_EMAIL("documents_composition_templates/NotificationAARForEMAIL.html"),
        AAR_NOTIFICATION_EMAIL_DE("documents_composition_templates/NotificationAARForEMAIL_de.html"),
        AAR_NOTIFICATION_EMAIL_SL("documents_composition_templates/NotificationAARForEMAIL_sl.html"),
        AAR_NOTIFICATION_EMAIL_FR("documents_composition_templates/NotificationAARForEMAIL_fr.html"),


        AAR_NOTIFICATION_PEC("documents_composition_templates/NotificationAARForPEC.html"),
        AAR_NOTIFICATION_PEC_DE("documents_composition_templates/NotificationAARForPEC_de.html"),
        AAR_NOTIFICATION_PEC_SL("documents_composition_templates/NotificationAARForPEC_sl.html"),
        AAR_NOTIFICATION_PEC_FR("documents_composition_templates/NotificationAARForPEC_fr.html"),

        NOTIFICATION_CANCELLED("documents_composition_templates/NotificationCancelledLegalFact.html"),
        NOTIFICATION_CANCELLED_DE("documents_composition_templates/NotificationCancelledLegalFact_de.html"),
        NOTIFICATION_CANCELLED_SL("documents_composition_templates/NotificationCancelledLegalFact_sl.html"),
        NOTIFICATION_CANCELLED_FR("documents_composition_templates/NotificationCancelledLegalFact_fr.html"),

        AAR_NOTIFICATION_SUBJECT("documents_composition_templates/NotificationAARSubject.txt"),
        AAR_NOTIFICATION_SMS("documents_composition_templates/NotificationAARForSMS.txt");
        AAR_NOTIFICATION_SMS("documents_composition_templates/NotificationAARForSMS.txt");

        private final String htmlTemplate;

        TemplateType(String htmlTemplate) {
            this.htmlTemplate = htmlTemplate;
        }

        public String getHtmlTemplate() {
            return htmlTemplate;
        }
    }

    private final Map<TemplateType, String> baseUris;
    private final Configuration freemarker;

    private final HtmlSanitizer htmlSanitizer;

    public DocumentComposition(Configuration freemarker, HtmlSanitizer htmlSanitizer) throws IOException {
        this.freemarker = freemarker;
        this.htmlSanitizer = htmlSanitizer;

        log.info("Preload templates START");
        baseUris = new EnumMap<>(TemplateType.class);
        StringTemplateLoader stringLoader = new StringTemplateLoader();

        for( TemplateType templateType : TemplateType.values() ) {
            log.info(" - begin to preload template with templateType={}", templateType );
            BaseUriAndTemplateBody info = preloadTemplate( templateType );

            this.baseUris.put( templateType, info.getBaseUri() );
            stringLoader.putTemplate( templateType.name(), info.templateBody);
        }
        log.debug("Configure freemarker ... ");
        this.freemarker.setTemplateLoader( stringLoader );
        log.debug(" ... freemarker configured.");
        log.info("Preload templates END");
    }


    @Value
    private static class BaseUriAndTemplateBody {
        private String baseUri;
        private String templateBody;
    }

    private static BaseUriAndTemplateBody preloadTemplate( TemplateType templateType ) throws IOException {
        log.debug("Start pre-loading template with templateType={}", templateType);

        String templateResourceName = templateType.getHtmlTemplate();
        URL templateUrl = getClasspathResourceURL( templateResourceName );
        log.debug("Template with templateResourceName={} located at URL={}", templateResourceName, templateUrl );

        String baseUri = templateUrl.toString().replaceFirst("/[^/]*$", "/");
        String templateBody = loadTemplateBody( templateUrl );

        log.debug("Template resources baseUri={}", baseUri);
        return new BaseUriAndTemplateBody( baseUri, templateBody );
    }

    private static String loadTemplateBody( URL templateUrl ) throws IOException {

        String templateContent;
        try( InputStream templateIn = templateUrl.openStream()) {
            templateContent = StreamUtils.copyToString( templateIn, StandardCharsets.UTF_8 );
        } catch (IOException exc) {
            log.error("Loading Document Composition Template " + templateUrl, exc );
            throw exc;
        }
        return templateContent;
    }

    @Nullable
    private static URL getClasspathResourceURL( String resourceName ) {
        return Thread.currentThread().getContextClassLoader().getResource( resourceName );
    }

    public String executeTextTemplate( TemplateType templateType, Object model) {
        log.info("Execute templateType={} START", templateType );
        StringWriter stringWriter = new StringWriter();

        try {
            Template template = freemarker.getTemplate( templateType.name() );
            template.process( model, stringWriter );

        } catch (IOException | TemplateException exc) {
            throw new PnInternalException(
                    "Processing template " + templateType,
                    ERROR_CODE_DELIVERYPUSH_DOCUMENTCOMPOSITIONFAILED,
                    exc);
        }

        log.info("Execute templateType={} END", templateType );
        return stringWriter.getBuffer().toString();
    }

    public byte[] executePdfTemplate( TemplateType templateType, Object model ) throws IOException {
        Object trustedTemplateModel = htmlSanitizer.sanitize(model);
        String html = executeTextTemplate( templateType, trustedTemplateModel );

        String baseUri = baseUris.get( templateType );
        log.info("Pdf conversion start for templateType={} with baseUri={}", templateType, baseUri);

        byte[] pdf = html2Pdf( baseUri, html );

        log.info("Pdf conversion done");
        return pdf;
    }

    private byte[] html2Pdf( String baseUri, String html ) throws IOException {

        Document jsoupDoc = Jsoup.parse(html); // org.jsoup.nodes.Document
        W3CDom w3cDom = new W3CDom(); // org.jsoup.helper.W3CDom
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.usePdfUaAccessbility(true);
        builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_A);
        builder.withW3cDocument( w3cDoc, baseUri);
        
        builder.toStream(baos);
        builder.run();
        baos.close();
 
        return baos.toByteArray();
    }
    
    public int getNumberOfPageFromPdfBytes(byte[] pdf ){
        try(PDDocument document = PDDocument.load(pdf)){
            return document.getNumberOfPages();
        }catch (IOException ex){
            log.error("Exception in getNumberOfPageFromPdfBytes for pdf - ex", ex);
            throw new PnInternalException("Cannot get numberOfPages for pdf " + this.getClass(), ex.getMessage());
        }
    }

}
