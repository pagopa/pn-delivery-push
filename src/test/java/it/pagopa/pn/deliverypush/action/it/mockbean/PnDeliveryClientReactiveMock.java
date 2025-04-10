package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV25;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

@Slf4j
public class PnDeliveryClientReactiveMock implements PnDeliveryClientReactive {
    private final PnDeliveryClientMock pnDeliveryClientMock;

    public PnDeliveryClientReactiveMock(
    @Lazy PnDeliveryClientMock pnDeliveryClientMock) {
        this.pnDeliveryClientMock = pnDeliveryClientMock;
    }



    @Override
    public Mono<SentNotificationV25> getSentNotification(String iun) {
        return Mono.just(pnDeliveryClientMock.getSentNotification(iun));
    }

    @Override
    public Mono<Void> removeAllNotificationCostsByIun(String iun) {
        return Mono.empty();
    }
}
