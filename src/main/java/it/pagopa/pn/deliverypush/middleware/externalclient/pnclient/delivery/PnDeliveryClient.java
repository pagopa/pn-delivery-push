package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotification;
import java.util.Map;

public interface PnDeliveryClient {
    String CLIENT_NAME = "PN-DELIVERY";

    String UPDATE_STATUS = "UPDATE NOTIFICATION STATUS";
    String GET_NOTIFICATION = "GET NOTIFICATION";
    String GET_QUICK_ACCESS_TOKEN = "GET QUICK ACCESS TOKEN";

    void updateStatus(RequestUpdateStatusDto dto);
    SentNotification getSentNotification(String iun);
    Map<String, String> getQuickAccessLinkTokensPrivate(String iun);
}
