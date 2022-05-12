package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
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
    public Set<PaperNotificationFailed> getNotificationByRecipientId(String recipientId) {
        return paperNotificationsFailed.stream().filter(paperNotificationFailed -> recipientId.equals(paperNotificationFailed.getRecipientId())).collect(Collectors.toSet());
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        paperNotificationsFailed.remove(
                paperNotificationsFailed.stream().filter(paperNotificationFailed -> recipientId.equals(paperNotificationFailed.getRecipientId())
                        && iun.equals(paperNotificationFailed.getIun())).findFirst().get()
        );
    }
}
