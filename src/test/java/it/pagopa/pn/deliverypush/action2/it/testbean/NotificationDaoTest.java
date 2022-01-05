package it.pagopa.pn.deliverypush.action2.it.testbean;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;

import java.time.Instant;
import java.util.*;

public class NotificationDaoTest implements NotificationDao {
    private final Collection<Notification> notifications;

    public NotificationDaoTest( Collection<Notification> notifications ) {
        this.notifications = notifications;
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Notification> getNotificationByIun(String iun) {
        return notifications.stream().filter(notification -> notification.getIun().equals(iun)).findFirst();
    }

    @Override
    public List<NotificationSearchRow> searchNotification(boolean bySender, String senderReceiverId, Instant startDate, Instant endDate, String filterId, NotificationStatus status, String subjectRegExp) {
        throw new UnsupportedOperationException();
    }

}
