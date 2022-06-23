package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.mapper.NotificationMapper;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class PnDeliveryClientMock implements PnDeliveryClient {
    private Collection<SentNotification> notifications;

    public void clear() {
        this.notifications = new ArrayList<>();
    }

    public void addNotification(NotificationInt notification) {
        SentNotification sentNotification = NotificationMapper.internalToExternal(notification);
        this.notifications.add(sentNotification);
    }
    
    @Override
    public ResponseEntity<Void> updateStatus(it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto dto) {
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<SentNotification> getSentNotification(String iun) {
        Optional<SentNotification> sentNotificationOpt = notifications.stream().filter(notification -> iun.equals(notification.getIun())).findFirst();
        if(sentNotificationOpt.isPresent()){
            return ResponseEntity.ok(sentNotificationOpt.get());
        }
        throw new RuntimeException("Test error, iun is not presente in getSentNotification");
    }
}
