package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV25;

import java.util.Map;

public interface PnDeliveryClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DELIVERY;

    String GET_NOTIFICATION = "GET NOTIFICATION";
    String GET_QUICK_ACCESS_TOKEN = "GET QUICK ACCESS TOKEN";

    SentNotificationV25 getSentNotification(String iun);
    Map<String, String> getQuickAccessLinkTokensPrivate(String iun);
}
