package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.action2.NotificationViewedHandler;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TimelineDaoMock implements TimelineDao {
    public static final String SIMULATE_VIEW_NOTIFICATION= "simulate-view-notification";

    private Collection<TimelineElementInternal> timelineList;
    private final NotificationViewedHandler notificationViewedHandler;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;

    public TimelineDaoMock(@Lazy NotificationViewedHandler notificationViewedHandler, @Lazy NotificationService notificationService,
                           @Lazy NotificationUtils notificationUtils) {
        timelineList = new ArrayList<>();
        this.notificationViewedHandler = notificationViewedHandler;
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
    }

    public void clear() {
        this.timelineList = new ArrayList<>();
    }
    
    @Override
    public void addTimelineElement(TimelineElementInternal row) {
        if( row.getDetails() != null && row.getDetails().getRecIndex() != null ){
            
            NotificationRecipientInt notificationRecipientInt = getRecipientInt(row);
            String viewNotificationString = SIMULATE_VIEW_NOTIFICATION + row.getElementId();
            
            if(notificationRecipientInt.getTaxId().startsWith(viewNotificationString)){
                //Viene simulata la visualizzazione della notifica prima di uno specifico inserimento in timeline
                notificationViewedHandler.handleViewNotification( row.getIun(), row.getDetails().getRecIndex() );
            }
        }
        
        timelineList.add(row);
    }

    private NotificationRecipientInt getRecipientInt(TimelineElementInternal row) {
        NotificationInt notificationInt = this.notificationService.getNotificationByIun(row.getIun());
        return notificationUtils.getRecipientFromIndex(notificationInt, row.getDetails().getRecIndex());
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst();
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun) {
        return timelineList.stream().filter(timelineElement -> iun.equals(timelineElement.getIun())).collect(Collectors.toSet());
    }

    @Override
    public void deleteTimeline(String iun) {
        throw new UnsupportedOperationException();
    }
}
