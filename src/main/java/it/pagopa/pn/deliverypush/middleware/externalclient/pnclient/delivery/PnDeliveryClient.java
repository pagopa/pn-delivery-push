package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import java.util.Map;

public interface PnDeliveryClient {
    void updateStatus(RequestUpdateStatusDto dto);
    SentNotification getSentNotification(String iun);
    Map<String, String> getQuickAccessLinkTokensPrivate(String iun);
}
