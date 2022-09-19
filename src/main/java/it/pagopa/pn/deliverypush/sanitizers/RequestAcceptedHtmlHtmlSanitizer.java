package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

import java.util.Map;

import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.FIELD_NOTIFICATION;

public class RequestAcceptedHtmlHtmlSanitizer extends NotificationIntHtmlSanitizer {


    @Override
    public Map<String, Object> sanitize(Map<String, Object> templateModelMap) {
        NotificationInt notificationInt = (NotificationInt) templateModelMap.get(FIELD_NOTIFICATION);
        NotificationInt trustedNotificationInt = sanitize(notificationInt);

        templateModelMap.put("notification", trustedNotificationInt);
        return templateModelMap;

    }
}
