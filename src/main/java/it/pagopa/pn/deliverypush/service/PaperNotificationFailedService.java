package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;

import java.util.List;

public interface PaperNotificationFailedService {
    void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed);
    
    void deleteNotificationFailed(String recipientId, String iun);

    List<PaperNotificationFailed> getPaperNotificationByRecipientId(String recipientId);
}
