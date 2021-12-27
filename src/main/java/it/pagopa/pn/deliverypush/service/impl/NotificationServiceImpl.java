package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private NotificationDao notificationDao;

    public NotificationServiceImpl(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    @Override
    public Notification getNotificationByIun(String iun) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);
        if (optNotification.isPresent()) {
            return optNotification.get();
        } else {
            log.error("There isn't notification for iun {}", iun);
            throw new PnInternalException("There isn't notification for iun " + iun);
        }
    }

    @Override
    public NotificationRecipient getRecipientFromNotification(Notification notification, String taxId) {
        Optional<NotificationRecipient> optRec = notification.getRecipients().stream().filter(recipient -> taxId.equals(taxId)).findFirst();
        if (optRec.isPresent()) {
            return optRec.get();
        } else {
            log.error("There isn't recipient in notification for iun {} id", notification.getIun(), taxId);
            throw new PnInternalException("There isn't notification for iun " + notification.getIun() + " id " + taxId);
        }
    }
}
