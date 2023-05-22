package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import reactor.core.publisher.Mono;

public interface PnDeliveryClientReactive {
    String CLIENT_NAME = "PN-DELIVERY";
    String GET_NOTIFICATION = "GET NOTIFICATION";

    Mono<SentNotification> getSentNotification(String iun);
}
