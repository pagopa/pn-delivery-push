package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.*;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResult;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static it.pagopa.pn.deliverypush.service.impl.NotificationProcessCostServiceImpl.PAGOPA_NOTIFICATION_BASE_COST;

class NotificationProcessCostServiceImplTest {
    @Mock
    private TimelineService timelineService;
    @Mock
    private PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive;
    @Mock
    private PnDeliveryPushConfigs cfg;
    
    private NotificationProcessCostService service;

    @BeforeEach
    void setUp() {
        service = new NotificationProcessCostServiceImpl(timelineService, pnExternalRegistriesClientReactive, cfg);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getPagoPaNotificationBaseCost() {
        Integer notificationCost = 100;
        Mockito.when(cfg.getPagoPaNotificationBaseCost()).thenReturn(notificationCost);
        
        Integer pagoPaBaseCost = service.getPagoPaNotificationBaseCost().block();

        Assertions.assertEquals(notificationCost, pagoPaBaseCost);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void setNotificationStepCostOK() {
        //GIVEN
        int notificationStepCost = 100;
        String iun = "testIun";

        PaymentsInfoForRecipientInt paymentsInfoForRecipient = PaymentsInfoForRecipientInt.builder()
                .creditorTaxId("testCred")
                .noticeCode("testNotice")
                .recIndex(0)
                .build();
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = Collections.singletonList(paymentsInfoForRecipient);
        Instant eventTimestamp = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant eventStorageTimestamp = Instant.now().minus(1, ChronoUnit.HOURS);
        UpdateCostPhaseInt updateCostPhase = UpdateCostPhaseInt.VALIDATION;
        
        UpdateNotificationCostResponse updateNotificationCostResponse = new UpdateNotificationCostResponse();
        updateNotificationCostResponse.addUpdateResultsItem(
                new UpdateNotificationCostResult()
                        .creditorTaxId(paymentsInfoForRecipient.getCreditorTaxId())
                        .noticeCode(paymentsInfoForRecipient.getNoticeCode())
                        .recIndex(paymentsInfoForRecipient.getRecIndex())
                        .result(UpdateNotificationCostResult.ResultEnum.KO)
        );
        Mockito.when(pnExternalRegistriesClientReactive.updateNotificationCost(Mockito.any(UpdateNotificationCostRequest.class))).thenReturn(Mono.just(updateNotificationCostResponse));
        
        //WHEN
        UpdateNotificationCostResponseInt updateNotificationCostResponseInt = service.setNotificationStepCost(notificationStepCost,iun,paymentsInfoForRecipients,eventTimestamp,
                eventStorageTimestamp, updateCostPhase).block();
        
        //THEN
        Assertions.assertNotNull(updateNotificationCostResponseInt);
        Assertions.assertNotNull(updateNotificationCostResponseInt.getUpdateResults());
        Assertions.assertNotNull(updateNotificationCostResponseInt.getUpdateResults().get(0));
        
        UpdateNotificationCostResultInt updateNotificationCostResultInt = updateNotificationCostResponseInt.getUpdateResults().get(0);
        final UpdateNotificationCostResult updateNotificationCostResponseExpected = updateNotificationCostResponse.getUpdateResults().get(0);

        Assertions.assertEquals(updateNotificationCostResponseExpected.getResult().getValue(), updateNotificationCostResultInt.getResult().getValue());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getNoticeCode(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getNoticeCode());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getCreditorTaxId(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getCreditorTaxId());
        Assertions.assertEquals(updateNotificationCostResponseExpected.getRecIndex(), updateNotificationCostResultInt.getPaymentsInfoForRecipient().getRecIndex());
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_1() {
        // notifica singolo recipient
        // invio raccomandata semplice
        // nessun perfezionamento
        // DELIVERY_MODE

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

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex, NotificationFeePolicy.DELIVERY_MODE, true, 0).block();
        
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
        // DELIVERY_MODE

        String iun = "testIun";
        int recIndex = 0;

        TimelineElementInternal requestAccepted = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .details(new NotificationRequestAcceptedDetailsInt())
                .build();
        
        Set<TimelineElementInternal> timelineElements = new HashSet<>(List.of(requestAccepted));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex, NotificationFeePolicy.DELIVERY_MODE, true, 0).block();
        
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
        // DELIVERY_MODE

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

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex, NotificationFeePolicy.DELIVERY_MODE, true, null).block();

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
        // DELIVERY_MODE

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

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex, NotificationFeePolicy.DELIVERY_MODE, true, null).block();

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
        // DELIVERY_MODE
        
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

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex, NotificationFeePolicy.DELIVERY_MODE, true, null).block();

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
        // DELIVERY_MODE
        
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

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex0, NotificationFeePolicy.DELIVERY_MODE, true, null).block();

        int notificationProcessCostExpected = PAGOPA_NOTIFICATION_BASE_COST + analogCostRec0;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertEquals(notificationViewRec0.getTimestamp(), notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertNull(notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_7() {
        // notifica singolo recipient
        // un invio cartaceo
        // perfezionamento per decorrenza termini e per presa visione valorizzato
        // FLAT_RATE

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

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex, NotificationFeePolicy.FLAT_RATE, false, null).block();

        int notificationProcessCostExpected = 0;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertEquals(notificationView.getTimestamp(), notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertEquals(refinementDetails.getSchedulingDate(), notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_8() {
        // notifica singolo recipient
        // un invio cartaceo
        // perfezionamento per decorrenza termini e per presa visione valorizzato
        // FLAT_RATE

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

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(iun, recIndex, NotificationFeePolicy.DELIVERY_MODE, false, null).block();

        int notificationProcessCostExpected = 0;

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(notificationProcessCostExpected, notificationProcessCostResponse.getCost());
        Assertions.assertEquals(notificationView.getTimestamp(), notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertEquals(refinementDetails.getSchedulingDate(), notificationProcessCostResponse.getRefinementDate());
    }
}