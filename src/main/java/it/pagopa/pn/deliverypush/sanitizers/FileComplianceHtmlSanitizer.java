package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;

import java.util.Map;

import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.*;

public class FileComplianceHtmlSanitizer extends HtmlSanitizer {


    @Override
    public Map<String, Object> sanitize(Map<String, Object> templateModelMap) {
        String trustedFieldSignature = sanitize((String) templateModelMap.get(FIELD_SIGNATURE));
        String trustedFieldPdfFileName = sanitize((String) templateModelMap.get(FIELD_PDF_FILE_NAME));

        templateModelMap.put(FIELD_SIGNATURE, trustedFieldSignature);
        templateModelMap.put(FIELD_PDF_FILE_NAME, trustedFieldPdfFileName);
        return templateModelMap;
    }
}
