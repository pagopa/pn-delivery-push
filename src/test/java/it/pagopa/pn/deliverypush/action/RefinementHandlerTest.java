package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class RefinementHandlerTest {

    @Mock
    private TimelineService timelineService;
    
    @Mock
    private TimelineUtils timelineUtils;
    
    @Mock
    private NotificationService notificationService;
    
    @Mock
    private NotificationProcessCostService notificationProcessCostService;

    @Mock
    private AttachmentUtils attachmentUtils;

    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    private RefinementHandler refinementHandler;

    @BeforeEach
    public void setup() {
        refinementHandler = new RefinementHandler(timelineService,
                timelineUtils, notificationService, notificationProcessCostService, attachmentUtils, pnDeliveryPushConfigs);
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleRefinement() {
        String iun = "I01";
        Integer recIndex = 1;
        NotificationInt notification = getNotificationWithPhysicalAddress();

        when(pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).thenReturn(120);
        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(Boolean.FALSE);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notificationProcessCostService.getPagoPaNotificationBaseCost()).thenReturn(Mono.just(100));
        when(timelineUtils.buildRefinementTimelineElement(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(Instant.EPOCH.plusMillis(10)))).thenReturn(TimelineElementInternal.builder().build());
        when(attachmentUtils.changeAttachmentsRetention(notification, pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement())).thenReturn(Flux.empty());


        TimelineElementInternal scheduleRefinementTimelineElement = new TimelineElementInternal();
        scheduleRefinementTimelineElement.setCategory(TimelineElementCategoryInt.SCHEDULE_REFINEMENT);
        ScheduleRefinementDetailsInt scheduleRefinementDetailsInt = new ScheduleRefinementDetailsInt();
        scheduleRefinementDetailsInt.setSchedulingDate(Instant.EPOCH.plusMillis(10));
        scheduleRefinementTimelineElement.setDetails(scheduleRefinementDetailsInt);

        Mockito.when(timelineUtils.getScheduleRefinement(Mockito.anyString(), Mockito.anyInt())).thenReturn(Optional.of(scheduleRefinementTimelineElement));

        refinementHandler.handleRefinement(iun, recIndex);
        
        Mockito.verify(timelineUtils, Mockito.times(1)).buildRefinementTimelineElement(notification,
                recIndex, 100, true, Instant.EPOCH.plusMillis(10));
        
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleRefinementNotificationViewedAfterRefinement() {
        String iun = "I01";
        Integer recIndex = 1;
        NotificationInt notification = getNotificationWithPhysicalAddress();
        Instant now = Instant.now();

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(Boolean.TRUE);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notificationProcessCostService.getPagoPaNotificationBaseCost()).thenReturn(Mono.just(100));

        TimelineElementInternal viewedTimelineElement = new TimelineElementInternal();
        viewedTimelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        NotificationViewedCreationRequestDetailsInt notificationViewedCreationRequestDetailsInt = new NotificationViewedCreationRequestDetailsInt();
        notificationViewedCreationRequestDetailsInt.setEventTimestamp(now.plus(1l, ChronoUnit.DAYS));
        viewedTimelineElement.setDetails(notificationViewedCreationRequestDetailsInt);

        Mockito.when(timelineUtils.getNotificationViewCreationRequest(Mockito.anyString(), Mockito.anyInt())).thenReturn(Optional.of(viewedTimelineElement));

        TimelineElementInternal scheduleRefinementTimelineElement = new TimelineElementInternal();
        scheduleRefinementTimelineElement.setCategory(TimelineElementCategoryInt.SCHEDULE_REFINEMENT);
        ScheduleRefinementDetailsInt scheduleRefinementDetailsInt = new ScheduleRefinementDetailsInt();
        scheduleRefinementDetailsInt.setSchedulingDate(now);
        scheduleRefinementTimelineElement.setDetails(scheduleRefinementDetailsInt);

        Mockito.when(timelineUtils.getScheduleRefinement(Mockito.anyString(), Mockito.anyInt())).thenReturn(Optional.of(scheduleRefinementTimelineElement));

        refinementHandler.handleRefinement(iun, recIndex);

        Mockito.verify(timelineUtils, Mockito.times(1)).buildRefinementTimelineElement(notification,
                recIndex, 100,false, now);

    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void handleRefinementNotificationViewedBeforeRefinement() {
        String iun = "I01";
        Integer recIndex = 1;
        NotificationInt notification = getNotificationWithPhysicalAddress();

        Instant now = Instant.now();

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(Boolean.TRUE);

        TimelineElementInternal viewedTimelineElement = new TimelineElementInternal();
        viewedTimelineElement.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST);
        NotificationViewedCreationRequestDetailsInt notificationViewedCreationRequestDetailsInt = new NotificationViewedCreationRequestDetailsInt();
        notificationViewedCreationRequestDetailsInt.setEventTimestamp(now.minus(1l, ChronoUnit.DAYS));
        viewedTimelineElement.setDetails(notificationViewedCreationRequestDetailsInt);

        Mockito.when(timelineUtils.getNotificationViewCreationRequest(Mockito.anyString(), Mockito.anyInt())).thenReturn(Optional.of(viewedTimelineElement));

        TimelineElementInternal scheduleRefinementTimelineElement = new TimelineElementInternal();
        scheduleRefinementTimelineElement.setCategory(TimelineElementCategoryInt.SCHEDULE_REFINEMENT);
        ScheduleRefinementDetailsInt scheduleRefinementDetailsInt = new ScheduleRefinementDetailsInt();
        scheduleRefinementDetailsInt.setSchedulingDate(now);
        scheduleRefinementTimelineElement.setDetails(scheduleRefinementDetailsInt);

        Mockito.when(timelineUtils.getScheduleRefinement(Mockito.anyString(), Mockito.anyInt())).thenReturn(Optional.of(scheduleRefinementTimelineElement));

        refinementHandler.handleRefinement(iun, recIndex);

        Mockito.verify(timelineUtils, Mockito.never()).buildRefinementTimelineElement(notification,
                recIndex, 100,true, now);

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleRefinementError() {
        String iun = "I01";
        Integer recIndex = 1;
        NotificationInt notification = getNotificationWithPhysicalAddress();

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(Boolean.FALSE);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notificationProcessCostService.getPagoPaNotificationBaseCost()).thenReturn(Mono.error(new RuntimeException("questa è l'eccezione")));

        assertThrows(RuntimeException.class, () -> {
            refinementHandler.handleRefinement(iun, recIndex);
        });
    }
    
    private NotificationInt getNotificationWithPhysicalAddress() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(
                                        PhysicalAddressInt.builder()
                                                .address("test address")
                                                .build()
                                )
                                .payments(null)
                                .build()
                ))
                .build();
    }
}