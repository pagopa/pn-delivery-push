package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRequestAcceptedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RefinementDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleRefinementDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotificationNotAcceptedException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry_reactive.model.UpdateNotificationCostResult;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistriesClientReactive;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

class NotificationProcessCostServiceImplTest {
    @Mock
    private TimelineService timelineService;
    @Mock
    private PnExternalRegistriesClientReactive pnExternalRegistriesClientReactive;
    @Mock
    private PnDeliveryPushConfigs cfg;
    
    private NotificationProcessCostService service;

    Integer notificationCost = 100;
    Integer notificationFee = 99;
    Integer notificationVat = 22;
    @BeforeEach
    void setUp() {
        Mockito.when(cfg.getPagoPaNotificationBaseCost()).thenReturn(notificationCost);
        Mockito.when(cfg.getPagoPaNotificationFee()).thenReturn(notificationFee);
        Mockito.when(cfg.getPagoPaNotificationVat()).thenReturn(notificationVat);

        service = new NotificationProcessCostServiceImpl(timelineService, pnExternalRegistriesClientReactive, cfg);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getPagoPaNotificationBaseCost() {
        Integer pagoPaBaseCost = service.getSendFeeAsync().block();

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
    void notificationProcessCostF24_vat_paFee_version23() {
        //GIVEN

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

        int paFee = 0;
        int vat = 22;
        String version = "2.3";

        //WHEN
        Integer notificationCost = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        ).block();

        //THEN
        int notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                vat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessTotalCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_notVat_paFee_version23() {
        //GIVEN

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

        int paFee = 0;
        Integer vat = null;
        String version = "2.3";

        //WHEN
        final Mono<Integer> notificationProcessCostMono = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        );

        Integer notificationCost  = notificationProcessCostMono.block();

        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_notVat_paFee_version21() {
        //GIVEN

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

        int paFee = 0;
        Integer vat = null;
        String version = "2.1";

        //WHEN
        Integer notificationCost = service.notificationProcessCostF24(
                        iun,
                        recIndex,
                        NotificationFeePolicy.DELIVERY_MODE,
                        paFee,
                        vat,
                        version
                ).block();


        //THEN
        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_notVat_paFee_notVersion() {
        //GIVEN

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

        int paFee = 0;
        Integer vat = null;
        String version = null;

        //WHEN
        Integer notificationCost = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        ).block();


        //THEN
        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                paFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCostF24_vat_notPaFee_version23() {
        //GIVEN

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

        Integer paFee = null;
        Integer vat = 22;
        String version = "2.3";

        //WHEN
        final Mono<Integer> notificationProcessCostMono = service.notificationProcessCostF24(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                paFee,
                vat,
                version
        );

        Integer notificationCost  = notificationProcessCostMono.block();

        int notificationProcessPartialCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                simpleRegisteredLetterCost,
                notificationFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationCost);
        Assertions.assertEquals(notificationCost, notificationProcessPartialCostExpected);
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_1() {
        //GIVEN
        
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
        
        int paFee = 0;
        int vat = 22;

        //WHEN
        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun, 
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE, 
                true,
                paFee,
                vat
        ).block();
        
        //THEN
        int notificationProcessPartialCostExpected = getNotificationProcessPartialCostExpected(
                service.getSendFee(), 
                simpleRegisteredLetterCost
        );
        int notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(), 
                simpleRegisteredLetterCost,
                paFee,
                vat
        );
        
        Assertions.assertNotNull(notificationProcessCostResponse);
        checkCost(notificationProcessCostResponse, notificationProcessPartialCostExpected, notificationProcessTotalCostExpected);
        checkCostData(notificationProcessCostResponse, simpleRegisteredLetterCost, vat, paFee);
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

        int paFee = 0;
        int vat = 22;

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE, 
                true,
                paFee,
                vat
        ).block();

        int notificationProcessPartialCostExpected = getNotificationProcessPartialCostExpected(
                service.getSendFee(),
                0
        );
        int notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                0,
                paFee,
                vat
        );

        Assertions.assertNotNull(notificationProcessCostResponse);
        checkCost(notificationProcessCostResponse, notificationProcessPartialCostExpected, notificationProcessTotalCostExpected);
        checkCostData(notificationProcessCostResponse, 0, vat, paFee);
        Assertions.assertNull(notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertNull(notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_3() {
        //GIVEN
        
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

        Instant refinementInstant = Instant.now();

        TimelineElementInternal refinement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REFINEMENT)
                .timestamp(refinementInstant)
                .details(RefinementDetailsInt.builder()
                        .recIndex(recIndex)
                        .eventTimestamp(refinementInstant)
                        .build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, firsAnalogSend, secondAnalogSend, refinement));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        //WHEN
        Integer vat = 22;
        Integer paFee = null;
        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun, 
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE, 
                true,
                paFee,
                vat
        ).block();
        
        //THEN
        int analogCost = firstAnalogCost + secondAnalogCost;

        int notificationProcessPartialCostExpected = getNotificationProcessPartialCostExpected(
                service.getSendFee(),
                analogCost
        );
        
        Integer notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                analogCost,
                notificationFee,
                vat
        );

        Assertions.assertNotNull(notificationProcessCostResponse);
        checkCost(notificationProcessCostResponse, notificationProcessPartialCostExpected, notificationProcessTotalCostExpected);
        checkCostData(notificationProcessCostResponse, analogCost, vat, notificationFee);

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
        
        //GIVEN
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

        //WHEN
        Integer paFee = null;
        Integer vat = null;
        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                true,
                paFee,
                vat
        ).block();
        
        //THEN
        int analogCost = firstAnalogCost;
        
        int notificationProcessPartialCostExpected = getNotificationProcessPartialCostExpected(
                service.getSendFee(),
                analogCost
        );

        Integer notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                analogCost,
                notificationFee,
                notificationVat
        );

        Assertions.assertNotNull(notificationProcessCostResponse);
        checkCost(notificationProcessCostResponse, notificationProcessPartialCostExpected, notificationProcessTotalCostExpected);
        checkCostData(notificationProcessCostResponse, analogCost, notificationVat, notificationFee);

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
        
        //GIVEN
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

        Instant instant = Instant.now().plus(Duration.ofSeconds(1000));
        TimelineElementInternal notificationView = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST)
                .timestamp(instant)
                .details(NotificationViewedCreationRequestDetailsInt.builder().eventTimestamp(instant).build())
                .build();
        
        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, firsAnalogSend, scheduleRefinement, notificationView));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);
        
        //WHEN
        Integer paFee = 22;
        Integer vat = 0;

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun, 
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                true, 
                paFee,
                vat
        ).block();
        
        //THEN
        int analogCost = firstAnalogCost;

        int notificationProcessPartialCostExpected = getNotificationProcessPartialCostExpected(
                service.getSendFee(),
                analogCost
        );

        Integer notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                analogCost,
                paFee,
                vat
        );

        Assertions.assertNotNull(notificationProcessCostResponse);
        checkCost(notificationProcessCostResponse, notificationProcessPartialCostExpected, notificationProcessTotalCostExpected);
        checkCostData(notificationProcessCostResponse, analogCost, vat, paFee);

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
        
        //GIVEN
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

        Instant instant = Instant.now();
        TimelineElementInternal notificationViewRec0 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST)
                .timestamp(instant)
                .details(NotificationViewedCreationRequestDetailsInt.builder()
                        .eventTimestamp(instant)
                        .recIndex(recIndex0)
                        .build())
                .build();

        TimelineElementInternal notificationViewRec1 = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST)
                .timestamp(Instant.now().plus(Duration.ofDays(1)))
                .details(NotificationViewedCreationRequestDetailsInt.builder()
                        .eventTimestamp(instant)
                        .recIndex(recIndex1)
                        .build())
                .build();
        
        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, analogSendRec0, analogSendRec1, notificationViewRec0, notificationViewRec1));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        //WHEN
        Integer paFee = null;
        Integer vat = 80;

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun, 
                recIndex0,
                NotificationFeePolicy.DELIVERY_MODE,
                true,
                paFee,
                vat
        ).block();
        
        int analogCost = analogCostRec0;

        int notificationProcessPartialCostExpected = getNotificationProcessPartialCostExpected(
                service.getSendFee(),
                analogCost
        );

        Integer notificationProcessTotalCostExpected = getNotificationProcessTotalCostExpected(
                service.getSendFee(),
                analogCost,
                notificationFee,
                vat
        );

        Assertions.assertNotNull(notificationProcessCostResponse);
        checkCost(notificationProcessCostResponse, notificationProcessPartialCostExpected, notificationProcessTotalCostExpected);
        checkCostData(notificationProcessCostResponse, analogCost, vat, notificationFee);
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
        
        //GIVEN
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

        Instant instant = Instant.now().plus(Duration.ofSeconds(1000));
        TimelineElementInternal notificationView = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST)
                .timestamp(instant)
                .details(NotificationViewedCreationRequestDetailsInt.builder().eventTimestamp(instant).build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, firsAnalogSend, scheduleRefinement, notificationView));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        //WHEN
        Integer paFee = 100;
        Integer vat = 10;

        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun,
                recIndex,
                NotificationFeePolicy.FLAT_RATE,
                false,
                paFee,
                vat
        ).block();
        
        //THEN
        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(0, notificationProcessCostResponse.getPartialCost());
        Assertions.assertEquals(0, notificationProcessCostResponse.getTotalCost());
        checkCostData(notificationProcessCostResponse, firstAnalogCost, vat, paFee);
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
        
        //GIVEN
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

        Instant instant = Instant.now().plus(Duration.ofSeconds(1000));
        TimelineElementInternal notificationView = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST)
                .timestamp(instant)
                .details(NotificationViewedCreationRequestDetailsInt.builder().eventTimestamp(instant).build())
                .build();

        Set<TimelineElementInternal> timelineElements = new HashSet<>(Arrays.asList(requestAccepted, firsAnalogSend, scheduleRefinement, notificationView));

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        //WHEN
        Integer paFee = 100;
        Integer vat = 10;
        NotificationProcessCost notificationProcessCostResponse = service.notificationProcessCost(
                iun,
                recIndex, 
                NotificationFeePolicy.DELIVERY_MODE, 
                false, 
                paFee,
                vat
        ).block();
        
        //THEN

        Assertions.assertNotNull(notificationProcessCostResponse);
        Assertions.assertEquals(0, notificationProcessCostResponse.getPartialCost());
        Assertions.assertEquals(0, notificationProcessCostResponse.getTotalCost());
        checkCostData(notificationProcessCostResponse, firstAnalogCost, vat, paFee);
        Assertions.assertEquals(notificationView.getTimestamp(), notificationProcessCostResponse.getNotificationViewDate());
        Assertions.assertEquals(refinementDetails.getSchedulingDate(), notificationProcessCostResponse.getRefinementDate());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void notificationProcessCost_NotificationNotAccepted() {
        // notifica singolo recipient non ancora accettata

        //GIVEN
        String iun = "testIun";
        int recIndex = 0;


        Set<TimelineElementInternal> timelineElements = new HashSet<>();

        Mockito.when(timelineService.getTimeline(iun, false))
                .thenReturn(timelineElements);

        //WHEN
        Integer paFee = 100;
        Integer vat = 10;

        //THEN
        Assertions.assertThrows(PnNotificationNotAcceptedException.class, () -> service.notificationProcessCost(
                iun,
                recIndex,
                NotificationFeePolicy.DELIVERY_MODE,
                false,
                paFee,
                vat
        ).block());

    }


    private static void checkCost(NotificationProcessCost notificationProcessCostResponse, 
                                  int notificationProcessPartialCostExpected, 
                                  Integer notificationProcessTotalCostExpected) {
        Assertions.assertEquals(notificationProcessPartialCostExpected, notificationProcessCostResponse.getPartialCost());
        Assertions.assertEquals(notificationProcessTotalCostExpected, notificationProcessCostResponse.getTotalCost());
    }

    private void checkCostData(NotificationProcessCost notificationProcessCostResponse, int analogCost, Integer vat, Integer paFee) {
        Assertions.assertEquals(analogCost, notificationProcessCostResponse.getAnalogCost());
        Assertions.assertEquals(service.getSendFee(), notificationProcessCostResponse.getSendFee());
        Assertions.assertEquals(vat, notificationProcessCostResponse.getVat());
        Assertions.assertEquals(paFee, notificationProcessCostResponse.getPaFee());
    }
    
    private static int getNotificationProcessPartialCostExpected(int pagoPaBaseCost, int analogCost) {
        return pagoPaBaseCost + analogCost;
    }

    private static Integer getNotificationProcessTotalCostExpected(int pagoPaBaseCost, int analogCost, Integer paFee, Integer vat) {
        if (paFee != null && vat != null){
            int analogCostWithVat = getAnalogCostWithVat(vat, analogCost);
            return pagoPaBaseCost + analogCostWithVat + paFee;
        }
        return null;
    }

    private static Integer getAnalogCostWithVat(Integer vat, Integer analogCost) {
        return vat != null ? analogCost + (analogCost * vat / 100) : null;
    }
}