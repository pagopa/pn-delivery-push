package it.pagopa.pn.deliverypush.pnclient.delivery;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.RequestUpdateStatusDto;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface PnDeliveryClient {
    ResponseEntity<Void> updateState(RequestUpdateStatusDto dto);
    Optional<Notification> getNotificationInfo( String iun, boolean withTimeline );
}
