package it.pagopa.pn.deliverypush.pnclient.delivery;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public interface PnDeliveryClient {
    ResponseEntity<ResponseUpdateStatusDto> updateState(RequestUpdateStatusDto dto);
    Optional<Notification> getNotificationInfo( String iun, boolean withTimeline );
}
