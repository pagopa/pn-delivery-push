package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV26;
import reactor.core.publisher.Mono;


public interface PnDeliveryClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DELIVERY;
    String GET_NOTIFICATION = "GET NOTIFICATION";

    Mono<SentNotificationV26> getSentNotification(String iun);
}
