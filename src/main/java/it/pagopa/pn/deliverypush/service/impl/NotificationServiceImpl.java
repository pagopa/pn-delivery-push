package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
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

    @Override
    public NotificationRecipient getRecipientFromNotification(Notification notification, String taxId) {
        Optional<NotificationRecipient> optRec = notification.getRecipients().stream().filter(recipient -> taxId.equals(recipient.getTaxId())).findFirst();
        if (optRec.isPresent()) {
            return optRec.get();
        } else {
            log.error("There isn't recipient in notification - iun {} id {}", notification.getIun(), taxId);
            throw new PnInternalException("There isn't notification - iun " + notification.getIun() + " id " + taxId);
        }
    }
}
