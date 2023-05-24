package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import reactor.core.publisher.Mono;

public class PnDeliveryClientReactiveMock implements PnDeliveryClientReactive {
    @Override
    public Mono<SentNotification> getSentNotification(String iun) {
        throw new UnsupportedOperationException();
    }
}
