package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV21;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface PnDeliveryClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DELIVERY;
    String GET_NOTIFICATION = "GET NOTIFICATION";
    String UPDATE_STATUS_NOTIFICATION = "UPDATE STATUS NOTIFICATION";
    String REMOVE_IUV = "REMOVE IUV";

    Mono<SentNotificationV21> getSentNotification(String iun);

    Mono<Void> updateStatus(String iun, NotificationStatusInt notificationStatusInt, Instant updateStatusTimestamp);

    Mono<Void> removeAllNotificationCostsByIun(String iun);
}
