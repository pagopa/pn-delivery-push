package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;

import java.util.Optional;

public interface CourtesyMessageService {
    void sendCourtesyMessage(Notification notification, NotificationRecipient notificationRecipient);

    Optional<SendCourtesyMessageDetails> getFirstSentCourtesyMessage(String iun, String taxId);

}
