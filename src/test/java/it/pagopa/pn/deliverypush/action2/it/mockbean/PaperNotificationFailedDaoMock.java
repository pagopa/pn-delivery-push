package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;

import java.util.Set;

public class PaperNotificationFailedDaoMock implements PaperNotificationFailedDao {
    @Override
    public void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<PaperNotificationFailed> getNotificationByRecipientId(String recipientId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        throw new UnsupportedOperationException();
    }
}
