package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
import it.pagopa.pn.deliverypush.action.it.utils.MethodExecutor;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class TimelineDaoMock implements TimelineDao {
    public static final String SIMULATE_VIEW_NOTIFICATION= "simulate-view-notification";
    public static final String SIMULATE_CANCEL_NOTIFICATION= "simulate-cancel-notification";
    public static final String SIMULATE_AFTER_CANCEL_NOTIFICATION= "simulate-after-cancel-notification";
    public static final String SIMULATE_RECIPIENT_WAIT = "simulate-recipient-wait";
    public static final String WAIT_SEPARATOR = "@@";

    private final NotificationViewedRequestHandler notificationViewedRequestHandler;
    private CopyOnWriteArrayList<TimelineElementInternal> timelineList;
    private final NotificationService notificationService;
    private final NotificationUtils notificationUtils;
    private final NotificationCancellationService notificationCancellationService;
    private final NotificationCancellationActionHandler notificationCancellationActionHandler;
    private final PnDeliveryClientMock pnDeliveryClientMock;

    public TimelineDaoMock(@Lazy NotificationViewedRequestHandler notificationViewedRequestHandler, @Lazy NotificationService notificationService,
                           @Lazy NotificationUtils notificationUtils, @Lazy NotificationCancellationService notificationCancellationService,
                           @Lazy NotificationCancellationActionHandler notificationCancellationActionHandler,
                           @Lazy PnDeliveryClientMock pnDeliveryClientMock) {
        this.notificationViewedRequestHandler = notificationViewedRequestHandler;
        this.notificationCancellationActionHandler = notificationCancellationActionHandler;
        timelineList = new CopyOnWriteArrayList<>();
        this.notificationService = notificationService;
        this.notificationUtils = notificationUtils;
        this.notificationCancellationService = notificationCancellationService;
        this.pnDeliveryClientMock = pnDeliveryClientMock;
    }

    public void clear() {
        this.timelineList = new CopyOnWriteArrayList<>();
    }

    private void checkAndAddTimelineElement(TimelineElementInternal dto) {
        log.debug("[TEST] Start checkAndAddTimelineElement {}", dto);

        if( dto.getDetails() != null && dto.getDetails() instanceof RecipientRelatedTimelineElementDetails){

            log.debug("[TEST] Ok details is present {}", dto);

            NotificationRecipientInt notificationRecipientInt = getRecipientInt(dto);
            String simulateViewNotificationString = SIMULATE_VIEW_NOTIFICATION + dto.getElementId();
            String simulateRecipientWaitString = SIMULATE_RECIPIENT_WAIT + dto.getElementId();
            String simulateCancelNotificationString = SIMULATE_CANCEL_NOTIFICATION + dto.getElementId();

            if(notificationRecipientInt.getTaxId().startsWith(simulateViewNotificationString)){
                log.debug("[TEST] Simulate view notification {}", dto);
                //Viene simulata la visualizzazione della notifica prima di uno specifico inserimento in timeline
                NotificationViewedInt notificationViewedInt = NotificationViewedInt.builder()
                        .iun(dto.getIun())
                        .recipientIndex(((RecipientRelatedTimelineElementDetails) dto.getDetails()).getRecIndex())
                        .viewedDate(Instant.now())
                        .build();
                notificationViewedRequestHandler.handleViewNotificationDelivery(notificationViewedInt);
            }else if(notificationRecipientInt.getTaxId().startsWith(simulateRecipientWaitString)){
                //Viene simulata l'attesa in un determinato stato (elemento di timeline) per uno specifico recipient. 
                // L'attesa dura fino all'inserimento in timeline di un determinato elemento per un altro recipient
                String waitForElementId = notificationRecipientInt.getTaxId().replaceFirst(".*" + WAIT_SEPARATOR, "");
                log.debug("[TEST] Wait for elementId {}", waitForElementId);

                MethodExecutor.waitForExecution(
                        () -> getTimelineElement(dto.getIun(), waitForElementId)
                );
                
            }else if(notificationRecipientInt.getTaxId().startsWith(simulateCancelNotificationString)){
                //Viene simulata la cancellazione della notifica prima di uno specifico inserimento in timeline
                log.debug("[TEST] Simulate cancel notification {}", dto);
                notificationCancellationService.startCancellationProcess( dto.getIun(), dto.getPaId(), CxTypeAuthFleet.PA, new ArrayList<>()).block();
                // bisogna anche generare l'action

                ThreadPool.start(new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    notificationCancellationActionHandler.continueCancellationProcess(dto.getIun());
                }));

            }
        }

        log.debug("[TEST] Add timeline element {}", dto);

/*        if(getTimelineElement(dto.getIun(), dto.getElementId()).isPresent()){
            log.error("TimelineElement is already present timelineElementId={}",dto.getElementId());
            throw new RuntimeException("TimelineElement is already present");
        }*/
        
        timelineList.add(dto);


        if( dto.getDetails() != null && dto.getDetails() instanceof RecipientRelatedTimelineElementDetails) {

            NotificationRecipientInt notificationRecipientInt = getRecipientInt(dto);
            String simulateAfterCancelNotificationString = dto.getElementId() + SIMULATE_AFTER_CANCEL_NOTIFICATION ;

            if (notificationRecipientInt.getTaxId().endsWith(simulateAfterCancelNotificationString)) {
                //Viene simulata la cancellazione della notifica DOPO di uno specifico inserimento in timeline
                log.debug("[TEST] Simulate after cancel notification {}", dto);
                notificationCancellationService.startCancellationProcess(dto.getIun(), dto.getPaId(), CxTypeAuthFleet.PA, new ArrayList<>()).block();
                // bisogna anche generare l'action

                ThreadPool.start(new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    notificationCancellationActionHandler.continueCancellationProcess(dto.getIun());
                }));
            }
        }
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
        log.info("[TEST] Searching timelineId={} in timelineIds={}", timelineId, timelineList.stream().map(TimelineElementInternal::getElementId).toList());
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst();
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId) {
        return timelineList.stream().filter(timelineElement -> timelineId.equals(timelineElement.getElementId()) && iun.equals(timelineElement.getIun())).findFirst();
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun) {
        log.info("[TEST] get timeline for iun={} in timelineIds={}", iun, timelineList.stream().map(TimelineElementInternal::getElementId).toList());

        return timelineList.stream()
                .filter(
                        timelineElement -> iun.equals(timelineElement.getIun())
                ).collect(Collectors.toSet());
    }

    @Override
    public Set<TimelineElementInternal> getTimelineStrongly(String iun) {
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
