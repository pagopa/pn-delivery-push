package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV21;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
public class PnDeliveryClientReactiveMock implements PnDeliveryClientReactive {


    private PnDeliveryClientMock pnDeliveryClientMock;

    public PnDeliveryClientReactiveMock(
    @Lazy PnDeliveryClientMock pnDeliveryClientMock) {
        this.pnDeliveryClientMock = pnDeliveryClientMock;
    }



    @Override
    public Mono<SentNotificationV21> getSentNotification(String iun) {
        return Mono.just(pnDeliveryClientMock.getSentNotification(iun));
    }

    @Override
    public Mono<Void> updateStatus(String iun, NotificationStatusInt notificationStatusInt, Instant updateStatusTimestamp) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        return Mono.empty();
    }
}
