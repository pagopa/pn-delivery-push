package it.pagopa.pn.deliverypush.sanitizers;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;

import java.util.Map;

import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.FIELD_NOTIFICATION;
import static it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator.FIELD_RECIPIENT;

public class AARNotificationHtmlSanitizer extends NotificationIntSanitizer {


    @Override
    public Map<String, Object> sanitize(Map<String, Object> templateModelMap) {
        NotificationInt notificationInt = (NotificationInt) templateModelMap.get(FIELD_NOTIFICATION);
        NotificationRecipientInt recipient = (NotificationRecipientInt) templateModelMap.get(FIELD_RECIPIENT);

        NotificationInt trustedNotificationInt = sanitize(notificationInt);
        NotificationRecipientInt trustedRecipient = sanitize(recipient);

        templateModelMap.put(FIELD_NOTIFICATION, trustedNotificationInt);
        templateModelMap.put(FIELD_RECIPIENT, trustedRecipient);

        return templateModelMap;

    }

}
