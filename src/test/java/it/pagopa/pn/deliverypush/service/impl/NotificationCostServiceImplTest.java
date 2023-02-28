package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static it.pagopa.pn.deliverypush.service.impl.NotificationCostServiceImpl.PAGOPA_NOTIFICATION_BASE_COST;

class NotificationCostServiceImplTest {
    @Mock
    private PnDeliveryClient pnDeliveryClient;
    @Mock
    private TimelineService timelineService;

    private NotificationCostService service;
    
    @BeforeEach
    void setUp() {
        service = new NotificationCostServiceImpl(pnDeliveryClient, timelineService);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getPagoPaNotificationBaseCost() {
        Integer pagoPaBaseCost = service.getPagoPaNotificationBaseCost().block();

        Assertions.assertEquals(100, pagoPaBaseCost);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_1() {
        // notifica singolo recipient
        // invio raccomandata semplice
        // nessun perfezionamento
        
        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int simpleRegisteredLetterCost = 1400;
        
        TimelineElementInternal sendSimpleRegisteredLetter = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .analogCost(simpleRegisteredLetterCost)
                        .recIndex(recIndex)
                        .build())
                .build();
        
        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, sendSimpleRegisteredLetter));
                
        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex).block();
        
        int notificationProcessCostExpected = PAGOPA_NOTIFICATION_BASE_COST + simpleRegisteredLetterCost;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertNull(notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertNull(notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_2() {
        // notifica singolo recipient
        // nessun invio
        // nessun perfezionamento

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();
        
        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex).block();
        
        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(PAGOPA_NOTIFICATION_BASE_COST, notificationProcessCostResponse.getCost());
        Assertions.assertNull(notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertNull(notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_3() {
        // notifica singolo recipient
        // due invii cartacei
        // perfezionamento per decorrenza termini

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int firstAnalogCost = 1400;

        TimelineElementInternal firsAnalogSend = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder()
                        .analogCost(firstAnalogCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        final int secondAnalogCost = 1000;

        TimelineElementInternal secondAnalogSend = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder()
                        .analogCost(secondAnalogCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        TimelineElementInternal refinement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REFINEMENT)
                .timestamp(Instant.now())
                .details(RefinementDetailsInt.builder()
                        .recIndex(recIndex)
                        .build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, firsAnalogSend, secondAnalogSend, refinement));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex).block();

        int notificationProcessCostExpected = PAGOPA_NOTIFICATION_BASE_COST + firstAnalogCost + secondAnalogCost;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertNull(notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertEquals(refinement.getTimestamp(), notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_4() {
        // notifica singolo recipient
        // un invio cartaceo
        // scheduling del perfezionamento per decorrenza termini

        String iun = "testIun";
        int recIndex = 0;
        
        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int firstAnalogCost = 1400;

        TimelineElementInternal firsAnalogSend = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder()
                        .analogCost(firstAnalogCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        final ScheduleRefinementDetailsInt refinementDetails = ScheduleRefinementDetailsInt.builder()
                .schedulingDate(Instant.now().plus(Duration.ofDays(2)))
                .recIndex(recIndex)
                .build();
        TimelineElementInternal scheduleRefinement = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                .details(refinementDetails)
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, firsAnalogSend, scheduleRefinement));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex).block();

        int notificationProcessCostExpected = PAGOPA_NOTIFICATION_BASE_COST + firstAnalogCost;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertNull(notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertEquals(refinementDetails.getSchedulingDate(), notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_5() {
        // notifica singolo recipient
        // un invio cartaceo
        // perfezionamento per decorrenza termini e per presa visione valorizzato

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int firstAnalogCost = 1400;

        TimelineElementInternal firsAnalogSend = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder()
                        .analogCost(firstAnalogCost)
                        .recIndex(recIndex)
                        .build())
                .build();

        final ScheduleRefinementDetailsInt refinementDetails = ScheduleRefinementDetailsInt.builder()
                .schedulingDate(Instant.now().plus(Duration.ofDays(1)))
                .recIndex(recIndex)
                .build();
        TimelineElementInternal scheduleRefinement = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                .details(refinementDetails)
                .build();

        TimelineElementInternal notificationView = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .timestamp(Instant.now().plus(Duration.ofSeconds(1000)))
                .details(NotificationViewedDetailsInt.builder().build())
                .build();
        
        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, firsAnalogSend, scheduleRefinement, notificationView));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex).block();

        int notificationProcessCostExpected = PAGOPA_NOTIFICATION_BASE_COST + firstAnalogCost;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertEquals(notificationView.getTimestamp(), notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertEquals(refinementDetails.getSchedulingDate(), notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_6() {
        // notifica due recipient
        // un invio cartaceo per ogni recipient 
        // perfezionamento per presa visione per entrambi i recipient

        String iun = "testIun";
        int recIndex0 = 0;
        int recIndex1 = 1;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();

        final int analogCostRec0 = 1400;
        TimelineElementInternal analogSendRec0 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder()
                        .analogCost(analogCostRec0)
                        .recIndex(recIndex0)
                        .build())
                .build();

        final int analogCostRec1 = 2000;
        TimelineElementInternal analogSendRec1 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder()
                        .analogCost(analogCostRec1)
                        .recIndex(recIndex1)
                        .build())
                .build();

        TimelineElementInternal notificationViewRec0 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .timestamp(Instant.now())
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(recIndex0)
                        .build())
                .build();

        TimelineElementInternal notificationViewRec1 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .timestamp(Instant.now().plus(Duration.ofDays(1)))
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(recIndex1)
                        .build())
                .build();
        
        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, analogSendRec0, analogSendRec1, notificationViewRec0, notificationViewRec1));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex0).block();

        int notificationProcessCostExpected = PAGOPA_NOTIFICATION_BASE_COST + analogCostRec0;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertEquals(notificationViewRec0.getTimestamp(), notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertNull(notificationProcessCostResponse.getRefinementDate());
    }
}