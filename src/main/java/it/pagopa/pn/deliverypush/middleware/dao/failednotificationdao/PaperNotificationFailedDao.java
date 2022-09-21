package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao;

import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;

import java.util.Set;

public interface PaperNotificationFailedDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.failed-notification";

    void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed);

    Set<PaperNotificationFailed> getPaperNotificationFailedByRecipientId(String recipientId);

    void deleteNotificationFailed(String recipientId, String iun);

}
