package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final PnDeliveryClient pnDeliveryClient;

    public NotificationServiceImpl(PnDeliveryClient pnDeliveryClient) {
        this.pnDeliveryClient = pnDeliveryClient;
    }

    @Override
    public Notification getNotificationByIun(String iun) {
        Optional<Notification> optNotification = pnDeliveryClient.getNotificationInfo( iun, false );
        if (optNotification.isPresent()) {
            return optNotification.get();
        } else {
            log.error("There isn't notification for iun {}", iun);
            throw new PnInternalException("There isn't notification for iun " + iun);
        }
    }

}
