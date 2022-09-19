package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;

public class HtmlSanitizerFactory {

    public static HtmlSanitizer makeSanitizer(DocumentComposition.TemplateType templateType) {
        switch (templateType) {
            case REQUEST_ACCEPTED:
                return new RequestAcceptedHtmlHtmlSanitizer();
            case NOTIFICATION_VIEWED:
                return new NotificationViewHtmlSanitizer();
            case DIGITAL_NOTIFICATION_WORKFLOW:
                return new DigitalNotificationWorkFlowHtmlSanitizer();
            case FILE_COMPLIANCE:
                return new FileComplianceHtmlSanitizer();
            case AAR_NOTIFICATION:
                return new AARNotificationHtmlHtmlSanitizer();
            default: return getDefault();
        }
    }

    public static HtmlSanitizer getDefault() {
        return new DefaultHtmlSanitize();
    }
}
