package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class PnDeliveryClientReactiveMock implements PnDeliveryClientReactive {
    @Override
    public Mono<SentNotification> getSentNotification(String iun) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> updateStatus(String iun, NotificationStatusInt notificationStatusInt, Instant updateStatusTimestamp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        throw new UnsupportedOperationException();
    }
}
