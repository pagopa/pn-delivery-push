package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class PaperNotificationFailedDaoMock implements PaperNotificationFailedDao {
    private Collection<PaperNotificationFailed> paperNotificationsFailed;
    
    public PaperNotificationFailedDaoMock(Collection<PaperNotificationFailed> paperNotificationsFailed) {
        this.paperNotificationsFailed = paperNotificationsFailed;
    }

    public void clear() {
        this.paperNotificationsFailed = new ArrayList<>();
    }
    
    @Override
    public void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed) {
        this.paperNotificationsFailed.add(paperNotificationFailed);
    }

    @Override
    public Set<PaperNotificationFailed> getPaperNotificationFailedByRecipientId(String recipientId) {
        Collection<PaperNotificationFailed> paperNotificationsFailedCopy = new ArrayList<>(paperNotificationsFailed);
        return paperNotificationsFailedCopy.stream().filter(paperNotificationFailed -> recipientId.equals(paperNotificationFailed.getRecipientId())).collect(Collectors.toSet());
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        Collection<PaperNotificationFailed> paperNotificationsFailedCopy = new ArrayList<>(paperNotificationsFailed);
        paperNotificationsFailedCopy.stream().filter(
                paperNotificationFailed -> recipientId.equals(paperNotificationFailed.getRecipientId()) && iun.equals(paperNotificationFailed.getIun()))
                .findFirst()
                .ifPresent(notificationFailed -> paperNotificationsFailed.remove(notificationFailed));
    }
}
