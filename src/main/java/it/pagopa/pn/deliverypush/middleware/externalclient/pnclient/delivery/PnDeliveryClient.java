package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface PnDeliveryClient {
    ResponseEntity<Void> updateStatus(RequestUpdateStatusDto dto);
    ResponseEntity<SentNotification> getSentNotification(String iun);
    ResponseEntity<NotificationCostResponse> getNotificationCostPrivate(String paTaxId, String noticeCode);
    ResponseEntity<Map<String, String>> getQuickAccessLinkTokensPrivate(String iun);
}
