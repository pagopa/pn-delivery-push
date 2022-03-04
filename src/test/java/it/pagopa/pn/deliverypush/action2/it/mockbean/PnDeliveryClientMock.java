package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class PnDeliveryClientMock implements PnDeliveryClient {
    private Collection<Notification> notifications;

    public void clear() {
        this.notifications = new ArrayList<>();
    }

    @Override
    public ResponseEntity<ResponseUpdateStatusDto> updateState(RequestUpdateStatusDto dto) {
        return null;
    }

    @Override
    public Optional<Notification> getNotificationInfo(String iun, boolean withTimeline) {
        return notifications.stream().filter(notification -> iun.equals(notification.getIun())).findFirst();
    }

    public void addNotification(Notification notification) {
        this.notifications.add(notification);
    }

}
