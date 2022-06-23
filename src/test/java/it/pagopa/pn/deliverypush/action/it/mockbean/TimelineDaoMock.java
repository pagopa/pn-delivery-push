package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.NotificationViewedHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

@Slf4j
public class TimelineDaoMock implements TimelineDao {
    public static final String SIMULATE_VIEW_NOTIFICATION= "simulate-view-notification";
    public static final String SIMULATE_RECIPIENT_WAIT = "simulate-recipient-wait";
    public static final String WAIT_SEPARATOR = "@@";

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
    public void addTimelineElement(TimelineElementInternal dto) {
        
        if( dto.getDetails() != null && dto.getDetails() instanceof RecipientRelatedTimelineElementDetails){
            
            NotificationRecipientInt notificationRecipientInt = getRecipientInt(dto);
            String simulateViewNotificationString = SIMULATE_VIEW_NOTIFICATION + dto.getElementId();
            String simulateRecipientWaitString = SIMULATE_RECIPIENT_WAIT + dto.getElementId();

            if(notificationRecipientInt.getTaxId().startsWith(simulateViewNotificationString)){
                //Viene simulata la visualizzazione della notifica prima di uno specifico inserimento in timeline
                notificationViewedHandler.handleViewNotification( dto.getIun(), ((RecipientRelatedTimelineElementDetails) dto.getDetails()).getRecIndex() );
            }else if(notificationRecipientInt.getTaxId().startsWith(simulateRecipientWaitString)){
                //Viene simulata l'attesa in un determinato stato (elemento di timeline) per uno specifico recipient. 
                // L'attesa dura fino all'inserimento in timeline di un determinato elemento per un altro recipient
                String waitForElementId = notificationRecipientInt.getTaxId().replaceFirst(".*" + WAIT_SEPARATOR, "");
                await().untilAsserted(() ->
                        Assertions.assertTrue(getTimelineElement(dto.getIun(), waitForElementId).isPresent())
                );
            }
        }
        
        timelineList.add(dto);
    }

    private NotificationRecipientInt getRecipientInt(TimelineElementInternal row) {
        if(row.getDetails() instanceof RecipientRelatedTimelineElementDetails){
            NotificationInt notificationInt = this.notificationService.getNotificationByIun(row.getIun());
            return notificationUtils.getRecipientFromIndex(notificationInt, ((RecipientRelatedTimelineElementDetails) row.getDetails()).getRecIndex());
        }else {
            throw new PnInternalException("There isn't recipient index for timeline element");
        }
    }

    @Override
    public synchronized Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst();
    }

    @Override
    public synchronized Set<TimelineElementInternal> getTimeline(String iun) {
        return timelineList.stream()
                .filter(
                        timelineElement -> iun.equals(timelineElement.getIun())
                ).collect(Collectors.toSet());
    }

    @Override
    public void deleteTimeline(String iun) {
        throw new UnsupportedOperationException();
    }
}
