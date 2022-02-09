package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.model.notification.cassandra.NotificationEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class NotificationDaoMock implements NotificationDao {
    private final List<Notification> notifications;

    public NotificationDaoMock(Collection<Notification> notifications) {
        this.notifications = new ArrayList<>(notifications);;
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Notification> getNotificationByIun(String iun) {
        return notifications.stream().filter(notification -> iun.equals(notification.getIun())).findFirst();
    }

    @Override
    public List<NotificationSearchRow> searchNotification(boolean bySender, String senderReceiverId, Instant startDate, Instant endDate, String filterId, NotificationStatus status, String subjectRegExp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<NotificationEntity> updateNotification(Notification notification) {
        for(int i = 0 ; i < notifications.size() ; i++ ){
            Notification elem = notifications.get(i);
            if(elem.getIun().equals(notification.getIun())){
                notifications.set(i,notification);
                return Optional.of(NotificationEntity.builder().build());
            }
        }
        return Optional.empty();
    }

}
