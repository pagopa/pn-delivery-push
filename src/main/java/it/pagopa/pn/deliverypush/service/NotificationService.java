package it.pagopa.pn.deliverypush.service;

import java.time.Instant;
import java.util.Map;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import reactor.core.publisher.Mono;

public interface NotificationService {
    NotificationInt getNotificationByIun(String iun);

    Map<String, String> getRecipientsQuickAccessLinkToken(String iun);
    
    Mono<NotificationInt> getNotificationByIunReactive(String iun);

    Mono<Void> updateStatus(String iun, NotificationStatusInt notificationStatusInt, Instant updateStatusTimestamp);
}
