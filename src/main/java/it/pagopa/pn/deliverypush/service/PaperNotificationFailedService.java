package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;

import java.util.List;

public interface PaperNotificationFailedService {
    List<PaperNotificationFailed> getPaperNotificationsFailed(String recipientId);
}
