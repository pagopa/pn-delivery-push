package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;

import java.util.Map;

import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.FIELD_IUN;
import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.FIELD_RECIPIENT;

public class NotificationViewHtmlSanitizer extends HtmlSanitizer {


    @Override
    public Map<String, Object> sanitize(Map<String, Object> templateModelMap) {
        String trustedIun = sanitize((String) templateModelMap.get(FIELD_IUN));
        NotificationRecipientInt trustedNotificationRecipientInt = sanitize((NotificationRecipientInt) templateModelMap.get(FIELD_RECIPIENT));

        templateModelMap.put(FIELD_IUN, trustedIun);
        templateModelMap.put(FIELD_RECIPIENT, trustedNotificationRecipientInt);
        return templateModelMap;
    }
}
