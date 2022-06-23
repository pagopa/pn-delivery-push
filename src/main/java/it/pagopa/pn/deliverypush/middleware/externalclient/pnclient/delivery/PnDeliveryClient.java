package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import org.springframework.http.ResponseEntity;

public interface PnDeliveryClient {
    ResponseEntity<Void> updateStatus(RequestUpdateStatusDto dto);
    ResponseEntity<SentNotification> getSentNotification(String iun);
}
