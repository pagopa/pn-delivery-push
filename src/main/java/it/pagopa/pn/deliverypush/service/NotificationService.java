package it.pagopa.pn.deliverypush.service;

import java.util.Map;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;

public interface NotificationService {
    NotificationInt getNotificationByIun(String iun);

    Map<String, String> getRecipientsQuickAccessLinkToken(String iun);
}
