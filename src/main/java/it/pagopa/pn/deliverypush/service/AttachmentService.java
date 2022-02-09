package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.Notification;

public interface AttachmentService {
    Notification checkAttachmentsAndGetCompleteNotification(Notification notification);
}
