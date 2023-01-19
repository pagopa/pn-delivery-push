package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

@Slf4j
public class TimelineDaoMock implements TimelineDao {
    public static final String SIMULATE_VIEW_NOTIFICATION= "simulate-view-notification";
    public static final String SIMULATE_RECIPIENT_WAIT = "simulate-recipient-wait";
    public static final String WAIT_SEPARATOR = "@@";

    private final NotificationViewedRequestHandler notificationViewedRequestHandler;
    private CopyOnWriteArrayList<TimelineElementInternal> timelineList;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;

    public TimelineDaoMock(@Lazy NotificationViewedRequestHandler notificationViewedRequestHandler, @Lazy NotificationService notificationService,
                           @Lazy NotificationUtils notificationUtils) {
        this.notificationViewedRequestHandler = notificationViewedRequestHandler;
        timelineList = new CopyOnWriteArrayList<>();
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
    }

    public void clear() {
        this.timelineList = new CopyOnWriteArrayList<>();
    }

    private void checkAndAddTimelineElement(TimelineElementInternal dto) {
        log.debug("Start checkAndAddTimelineElement {}", dto);

        if( dto.getDetails() != null && dto.getDetails() instanceof RecipientRelatedTimelineElementDetails){

            log.debug("Ok details is present {}", dto);

            NotificationRecipientInt notificationRecipientInt = getRecipientInt(dto);
            String simulateViewNotificationString = SIMULATE_VIEW_NOTIFICATION + dto.getElementId();
            String simulateRecipientWaitString = SIMULATE_RECIPIENT_WAIT + dto.getElementId();

            if(notificationRecipientInt.getTaxId().startsWith(simulateViewNotificationString)){
                log.debug("Simulate view notification {}", dto);
                //Viene simulata la visualizzazione della notifica prima di uno specifico inserimento in timeline
                notificationViewedRequestHandler.handleViewNotificationDelivery( dto.getIun(), ((RecipientRelatedTimelineElementDetails) dto.getDetails()).getRecIndex(), null, Instant.now());
            }else if(notificationRecipientInt.getTaxId().startsWith(simulateRecipientWaitString)){
                //Viene simulata l'attesa in un determinato stato (elemento di timeline) per uno specifico recipient. 
                // L'attesa dura fino all'inserimento in timeline di un determinato elemento per un altro recipient
                String waitForElementId = notificationRecipientInt.getTaxId().replaceFirst(".*" + WAIT_SEPARATOR, "");
                log.debug("Wait for elementId {}", waitForElementId);

                await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                        Assertions.assertTrue(getTimelineElement(dto.getIun(), waitForElementId).isPresent())
                );
            }
        }

        log.debug("Add timeline element {}", dto);

        timelineList.add(dto);
    }
    
    public void addTimelineElement(TimelineElementInternal dto){
        timelineList.add(dto);
    }
    
    @Override
    public void addTimelineElementIfAbsent(TimelineElementInternal dto) {
        checkAndAddTimelineElement(dto);
    }

    private NotificationRecipientInt getRecipientInt(TimelineElementInternal row) {
        if(row.getDetails() instanceof RecipientRelatedTimelineElementDetails){
            NotificationInt notificationInt = this.notificationService.getNotificationByIun(row.getIun());
            return notificationUtils.getRecipientFromIndex(notificationInt, ((RecipientRelatedTimelineElementDetails) row.getDetails()).getRecIndex());
        }else {
            throw new PnInternalException("There isn't recipient index for timeline element", "test");
        }
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst();
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun) {
        return timelineList.stream()
                .filter(
                        timelineElement -> iun.equals(timelineElement.getIun())
                ).collect(Collectors.toSet());
    }

    @Override
    public Set<TimelineElementInternal> getTimelineFilteredByElementId(String iun, String timelineId) {
        return timelineList.stream()
                .filter(
                        timelineElement -> iun.equals(timelineElement.getIun()) && timelineElement.getElementId().startsWith(timelineId)
                ).collect(Collectors.toSet());
    }

    @Override
    public void deleteTimeline(String iun) {
        throw new UnsupportedOperationException();
    }
}
