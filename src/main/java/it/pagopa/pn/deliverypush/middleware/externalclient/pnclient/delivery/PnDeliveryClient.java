package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV26;

public interface PnDeliveryClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DELIVERY;

    String GET_NOTIFICATION = "GET NOTIFICATION";

    SentNotificationV26 getSentNotification(String iun);
}
