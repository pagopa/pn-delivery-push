package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineCounterEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineCounterEntity;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeLineServiceImplTest {
    private TimelineDao timelineDao;
    private TimelineCounterEntityDao timelineCounterDao;
    private StatusUtils statusUtils;
    private TimeLineServiceImpl timeLineService;
    private StatusService statusService;
    private ConfidentialInformationService confidentialInformationService;
    private SchedulerService schedulerService;

    private NotificationService notificationService;

    @BeforeEach
    void setup() {
        timelineDao = Mockito.mock( TimelineDao.class );
        timelineCounterDao = Mockito.mock( TimelineCounterEntityDao.class );
        statusUtils = Mockito.mock( StatusUtils.class );
        statusService = Mockito.mock( StatusService.class );

        confidentialInformationService = Mockito.mock( ConfidentialInformationService.class );
        schedulerService = Mockito.mock(SchedulerService.class);
        notificationService = Mockito.mock(NotificationService.class);
//        timeLineService = new TimeLineServiceImpl(timelineDao , timelineCounterDao , statusUtils, confidentialInformationService, statusService, schedulerService, notificationService);
        timeLineService = new TimeLineServiceImpl(timelineDao , timelineCounterDao , statusUtils, confidentialInformationService, statusService, notificationService);
        //timeLineService.setSchedulerService(schedulerService);

    }

    @Test
    void addTimelineElement(){
        //GIVEN
        String iun = "iun_12345";
        String elementId = "elementId_12345";

        NotificationInt notification = getNotification(iun);
        StatusService.NotificationStatusUpdate notificationStatuses = new StatusService.NotificationStatusUpdate(NotificationStatusInt.ACCEPTED, NotificationStatusInt.ACCEPTED);
        Mockito.when(statusService.getStatus(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(notificationStatuses);
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String elementId2 = "elementId2";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId2);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(setTimelineElement);

        Instant timestampLastElementInTimeline = setTimelineElement.iterator().next().getTimestamp();
        StatusInfoInternal expectedStatusInfo = StatusInfoInternal.builder()
                .actual(NotificationStatusInt.ACCEPTED.getValue())
                .statusChangeTimestamp(timestampLastElementInTimeline).build();

        TimelineElementInternal newElement = getAarGenerationTimelineElement(iun, elementId);

        //WHEN
        timeLineService.addTimelineElement(newElement, notification);
        
        //THEN
        //mi aspetto che il timestampLastUpdateStatus sia null quando gli elementi già salvati non hanno valorizzato
        //lo statusInfo e non c'è stato un cambio di stato
        StatusInfoInternal actualStatusInfo = timeLineService.buildStatusInfo(notificationStatuses, null);
        TimelineElementInternal dtoWithStatusInfo = newElement.toBuilder().statusInfo(actualStatusInfo).build();
        Assertions.assertEquals(expectedStatusInfo.getActual(), actualStatusInfo.getActual());
        Assertions.assertEquals(expectedStatusInfo.isStatusChanged(), actualStatusInfo.isStatusChanged());
        Assertions.assertNull(actualStatusInfo.getStatusChangeTimestamp());
        Mockito.verify(timelineDao).addTimelineElementIfAbsent(dtoWithStatusInfo);
        Mockito.verify(statusService).getStatus(newElement, setTimelineElement, notification);
        Mockito.verify(confidentialInformationService).saveTimelineConfidentialInformation(newElement);
    }

    @Test
    void addTimelineElementError(){
        //GIVEN
        String iun = "iun";
        String elementId = "elementId";

        NotificationInt notification = getNotification(iun);
        
        String elementId2 = "elementId";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId2);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(setTimelineElement);

        TimelineElementInternal newElement = getSendPaperFeedbackTimelineElement(iun, elementId, Instant.now());

        Mockito.doThrow(new PnInternalException("error", "test")).when(statusService).getStatus(Mockito.any(TimelineElementInternal.class), Mockito.anySet(), Mockito.any(NotificationInt.class));

        // WHEN
        assertThrows(PnInternalException.class, () -> {
            timeLineService.addTimelineElement(newElement, notification);
        });
    }

    @Test
    void addTimelineElementWithChangedStatus(){
        //GIVEN
        String iun = "iun";
        String elementId = "elementId";

        String expectedNewStatus = NotificationStatusInt.ACCEPTED.getValue();
        boolean expectedStatusChanged = true;

        NotificationInt notification = getNotification(iun);
        StatusService.NotificationStatusUpdate notificationStatuses = new StatusService.NotificationStatusUpdate(NotificationStatusInt.IN_VALIDATION, NotificationStatusInt.ACCEPTED);
        Mockito.when(statusService.getStatus(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(notificationStatuses);
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String elementId2 = "elementId2";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId2);
        Set<TimelineElementInternal> timelineElementsWithStatusInfo = setTimelineElement.stream().map(timelineElementInternal -> timelineElementInternal.toBuilder()
                .statusInfo(StatusInfoInternal.builder()
                        .statusChangeTimestamp(Instant.now().minusSeconds(5))
                        .actual(NotificationStatusInt.IN_VALIDATION.getValue())
                        .build())
                .build()).collect(Collectors.toSet());

        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(timelineElementsWithStatusInfo);

        Instant timestampLastElementInTimeline = timelineElementsWithStatusInfo.iterator().next().getStatusInfo().getStatusChangeTimestamp();

        TimelineElementInternal newElement = getAarGenerationTimelineElement(iun, elementId);

        //WHEN
        timeLineService.addTimelineElement(newElement, notification);

        //THEN
        StatusInfoInternal actualStatusInfo = timeLineService.buildStatusInfo(notificationStatuses, timestampLastElementInTimeline);
        Assertions.assertEquals(expectedNewStatus, actualStatusInfo.getActual());
        Assertions.assertEquals(expectedStatusChanged, actualStatusInfo.isStatusChanged());
        Assertions.assertTrue(actualStatusInfo.getStatusChangeTimestamp().isAfter(timestampLastElementInTimeline));
    }

    @Test
    void addTimelineElementWithUnchangedStatus(){
        //GIVEN
        String iun = "iun";
        String elementId = "elementId";

        String expectedNewStatus = NotificationStatusInt.IN_VALIDATION.getValue();
        boolean expectedStatusChanged = false;

        NotificationInt notification = getNotification(iun);
        StatusService.NotificationStatusUpdate notificationStatuses = new StatusService.NotificationStatusUpdate(NotificationStatusInt.IN_VALIDATION, NotificationStatusInt.IN_VALIDATION);
        Mockito.when(statusService.getStatus(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(notificationStatuses);
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String elementId2 = "elementId2";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId2);
        Set<TimelineElementInternal> timelineElementsWithStatusInfo = setTimelineElement.stream().map(timelineElementInternal -> timelineElementInternal.toBuilder()
                .statusInfo(StatusInfoInternal.builder()
                        .statusChangeTimestamp(Instant.now().minusSeconds(5))
                        .actual(NotificationStatusInt.IN_VALIDATION.getValue())
                        .build())
                .build()).collect(Collectors.toSet());

        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(timelineElementsWithStatusInfo);

        Instant timestampLastElementInTimeline = timelineElementsWithStatusInfo.iterator().next().getStatusInfo().getStatusChangeTimestamp();

        TimelineElementInternal newElement = getAarGenerationTimelineElement(iun, elementId);

        //WHEN
        timeLineService.addTimelineElement(newElement, notification);

        //THEN
        StatusInfoInternal actualStatusInfo = timeLineService.buildStatusInfo(notificationStatuses, timestampLastElementInTimeline);
        Assertions.assertEquals(expectedNewStatus, actualStatusInfo.getActual());
        Assertions.assertEquals(expectedStatusChanged, actualStatusInfo.isStatusChanged());
        Assertions.assertEquals(timestampLastElementInTimeline, actualStatusInfo.getStatusChangeTimestamp());
    }


    @Test
    void getSendPaperFeedbackTimelineElement(){
        //GIVEN
        String iun = "iun";
        String timelineId = "idTimeline";

        TimelineElementInternal daoElement = getSendDigitalTimelineElement(iun, timelineId);

        Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(daoElement));

        ConfidentialTimelineElementDtoInt confidentialTimelineElementDtoInt = ConfidentialTimelineElementDtoInt.builder()
                        .timelineElementId(timelineId)
                        .digitalAddress("prova@prova.com")
                        .build();
        Mockito.when(confidentialInformationService.getTimelineElementConfidentialInformation(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(confidentialTimelineElementDtoInt));
         
        //WHEN
        Optional<TimelineElementInternal> retrievedElement = timeLineService.getTimelineElement(iun, timelineId);

        //THEN
        Assertions.assertTrue(retrievedElement.isPresent());
        Assertions.assertEquals(retrievedElement.get().getElementId(), daoElement.getElementId());
        Assertions.assertEquals( retrievedElement.get().getDetails(), daoElement.getDetails());

        SendDigitalDetailsInt details = (SendDigitalDetailsInt) retrievedElement.get().getDetails();
        Assertions.assertEquals(details.getDigitalAddress().getAddress(), confidentialTimelineElementDtoInt.getDigitalAddress());
    }

    @Test
    void getTimelineElementDetails(){
        //GIVEN
        String iun = "iun";
        String timelineId = "idTimeline";

        TimelineElementInternal daoElement = getSendDigitalTimelineElement(iun, timelineId);
        Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(daoElement));

        ConfidentialTimelineElementDtoInt confidentialTimelineElementDtoInt = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(timelineId)
                .digitalAddress("prova@prova.com")
                .build();
        Mockito.when(confidentialInformationService.getTimelineElementConfidentialInformation(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(confidentialTimelineElementDtoInt));

        //WHEN
        Optional<SendDigitalDetailsInt> detailsOpt = timeLineService.getTimelineElementDetails(iun, timelineId, SendDigitalDetailsInt.class);

        //THEN
        Assertions.assertTrue(detailsOpt.isPresent());
        SendDigitalDetailsInt details = detailsOpt.get();
        Assertions.assertEquals( daoElement.getDetails(), details);
        Assertions.assertEquals( daoElement.getDetails(), details);
        Assertions.assertEquals(confidentialTimelineElementDtoInt.getDigitalAddress(), details.getDigitalAddress().getAddress());
    }

    @Test
    void getTimelineElementDetailsEmpty(){
        //GIVEN
        String iun = "iun";
        String timelineId = "idTimeline";

        Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        //WHEN
        Optional<SendDigitalDetailsInt> detailsOpt = timeLineService.getTimelineElementDetails(iun, timelineId, SendDigitalDetailsInt.class);

        //THEN
        Assertions.assertFalse(detailsOpt.isPresent());
    }

    @Test
    void getTimelineElementWithoutConfidentialInformation(){
        //GIVEN
        String iun = "iun";
        String timelineId = "idTimeline";

        TimelineElementInternal daoElement = getScheduleAnalogWorkflowTimelineElement(iun, timelineId);
        Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(daoElement));
        
        Mockito.when(confidentialInformationService.getTimelineElementConfidentialInformation(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        //WHEN
        Optional<TimelineElementInternal> retrievedElement = timeLineService.getTimelineElement(iun, timelineId);

        //THEN
        Assertions.assertTrue(retrievedElement.isPresent());
        Assertions.assertEquals(retrievedElement.get().getElementId(), daoElement.getElementId());
        
        Assertions.assertEquals(retrievedElement.get().getDetails(), daoElement.getDetails());
    }

    @Test
    void getTimeline(){
        //GIVEN
        String iun = "iun";
        
        String timelineId1 = "idTimeline1";
        TimelineElementInternal scheduleAnalogNoConfInf = getScheduleAnalogWorkflowTimelineElement(iun, timelineId1);
        String timelineId2 = "idTimeline2";
        TimelineElementInternal sendDigitalConfInf = getSendDigitalTimelineElement(iun, timelineId2);
        String timelineId3 = "idTimeline3";
        TimelineElementInternal sendPaperFeedbackConfInf = getSendPaperFeedbackTimelineElement(iun, timelineId3, Instant.now());

        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        timelineElementList.add(scheduleAnalogNoConfInf);
        timelineElementList.add(sendDigitalConfInf);
        timelineElementList.add(sendPaperFeedbackConfInf);

        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(hashSet);

        Map<String, ConfidentialTimelineElementDtoInt> mapConfInf = new HashMap<>();
        ConfidentialTimelineElementDtoInt confInfDigital = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(timelineId2)
                .digitalAddress("prova@prova.com")
                .build();
        ConfidentialTimelineElementDtoInt confInfPhysical = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(timelineId3)
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .at("at")
                                .municipality("muni")
                                .province("NA")
                                .addressDetails("details")
                                .build()
                )
                .build();
        mapConfInf.put(confInfDigital.getTimelineElementId(), confInfDigital);
        mapConfInf.put(confInfPhysical.getTimelineElementId(), confInfPhysical);

        Mockito.when(confidentialInformationService.getTimelineConfidentialInformation(Mockito.anyString()))
                .thenReturn(Optional.of(mapConfInf));
        
        //WHEN
        Set<TimelineElementInternal> retrievedElements = timeLineService.getTimeline(iun, true);

        //THEN
        Assertions.assertFalse(retrievedElements.isEmpty());

        List<TimelineElementInternal> listElement = new ArrayList<>(retrievedElements);

        TimelineElementInternal retrievedScheduleAnalog = getSpecificElementFromList(listElement, scheduleAnalogNoConfInf.getElementId());
        Assertions.assertEquals(retrievedScheduleAnalog , scheduleAnalogNoConfInf);

        TimelineElementInternal retrievedSendDigital = getSpecificElementFromList(listElement, sendDigitalConfInf.getElementId());
        Assertions.assertNotNull(retrievedSendDigital);
        
        SendDigitalDetailsInt details = (SendDigitalDetailsInt) retrievedSendDigital.getDetails();
        Assertions.assertEquals(details, sendDigitalConfInf.getDetails());
        Assertions.assertEquals(details.getDigitalAddress().getAddress() , confInfDigital.getDigitalAddress());

        TimelineElementInternal retrievedSendPaperFeedback = getSpecificElementFromList(listElement, sendPaperFeedbackConfInf.getElementId());
        Assertions.assertNotNull(retrievedSendPaperFeedback);
        
        SendAnalogFeedbackDetailsInt details1 = (SendAnalogFeedbackDetailsInt) retrievedSendPaperFeedback.getDetails();
        Assertions.assertEquals(details1, sendPaperFeedbackConfInf.getDetails());
        Assertions.assertEquals(details1.getPhysicalAddress() , confInfPhysical.getPhysicalAddress());
    }

    @Test
    void getTimelineAndStatusHistory() {
        //GIVEN
        String iun = "iun";
        int numberOfRecipients1 = 1;
        Instant notificationCreatedAt = Instant.now();
        NotificationStatusInt currentStatus = NotificationStatusInt.DELIVERING;

        String elementId1 = "elementId1";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId1);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(setTimelineElement);

        Instant activeFromInValidation = Instant.now();
        
        NotificationStatusHistoryElementInt inValidationElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .activeFrom(activeFromInValidation)
                .build();

        Instant activeFromAccepted = activeFromInValidation.plus(Duration.ofDays(1));

        NotificationStatusHistoryElementInt acceptedElementElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom(activeFromAccepted)
                .build();

        Instant activeFromDelivering = activeFromAccepted.plus(Duration.ofDays(1));

        NotificationStatusHistoryElementInt deliveringElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERING)
                .activeFrom(activeFromDelivering)
                .build();
        
        List<NotificationStatusHistoryElementInt> notificationStatusHistoryElements = new ArrayList<>(List.of(inValidationElement, acceptedElementElement, deliveringElement));

        Mockito.when(
                statusUtils.getStatusHistory(Mockito.anySet() ,Mockito.anyInt(), Mockito.any(Instant.class))
        ).thenReturn(notificationStatusHistoryElements);

        Mockito.when(
                statusUtils.getCurrentStatus( Mockito.anyList() )
        ).thenReturn(currentStatus);

        //WHEN
        NotificationHistoryResponse notificationHistoryResponse = timeLineService.getTimelineAndStatusHistory(iun, numberOfRecipients1, notificationCreatedAt);

        //THEN
        
        //Viene verificato che il numero di elementi restituiti sia 2, dunque che sia stato eliminato l'elemento con category "IN VALIDATION"
        Assertions.assertEquals(2 , notificationHistoryResponse.getNotificationStatusHistory().size());
        
        NotificationStatusHistoryElement firstElement = notificationHistoryResponse.getNotificationStatusHistory().get(0);
        Assertions.assertEquals(acceptedElementElement.getStatus(), NotificationStatusInt.valueOf(firstElement.getStatus().getValue()) );
        Assertions.assertEquals(inValidationElement.getActiveFrom(), firstElement.getActiveFrom());

        NotificationStatusHistoryElement secondElement = notificationHistoryResponse.getNotificationStatusHistory().get(1);
        Assertions.assertEquals(deliveringElement.getStatus(), NotificationStatusInt.valueOf(secondElement.getStatus().getValue()));
        Assertions.assertEquals(deliveringElement.getActiveFrom(), secondElement.getActiveFrom());
        
        //Verifica timeline 
        List<TimelineElementInternal> timelineElementList = new ArrayList<>(setTimelineElement);
        TimelineElementInternal elementInt = timelineElementList.get(0);

        Assertions.assertEquals(timelineElementList.size() , notificationHistoryResponse.getTimeline().size());

        var firstElementReturned = notificationHistoryResponse.getTimeline().get(0);
        
        Assertions.assertEquals( notificationHistoryResponse.getNotificationStatus(), NotificationStatus.valueOf(currentStatus.getValue()) );
        Assertions.assertEquals( elementInt.getElementId(), firstElementReturned.getElementId() );
        
        SendAnalogDetailsInt details = (SendAnalogDetailsInt) elementInt.getDetails();
        Assertions.assertEquals( firstElementReturned.getDetails().getRecIndex(), details.getRecIndex());
        Assertions.assertEquals( firstElementReturned.getDetails().getPhysicalAddress().getAddress(), details.getPhysicalAddress().getAddress() );

    }


    @Test
    void getTimelineAndStatusHistoryOrder() {
        //GIVEN
        String iun = "iun";
        int numberOfRecipients1 = 1;
        Instant notificationCreatedAt = Instant.now();
        NotificationStatusInt currentStatus = NotificationStatusInt.DELIVERING;

        String elementId1 = "elementId1";
        Set<TimelineElementInternal> setTimelineElement = new HashSet<>();
        Instant t = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        TimelineElementInternal elementInternalFeedback = getSendPaperFeedbackTimelineElement(iun, elementId1+"FEEDBACK", t);
        setTimelineElement.add(elementInternalFeedback);
        TimelineElementInternal elementInternalProg = getSendPaperProgressTimelineElement(iun, elementId1+"PROGRESS", t);
        setTimelineElement.add(elementInternalProg);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(setTimelineElement);

        Instant activeFromInValidation = Instant.now();

        NotificationStatusHistoryElementInt inValidationElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .activeFrom(activeFromInValidation)
                .build();

        Instant activeFromAccepted = activeFromInValidation.plus(Duration.ofDays(1));

        NotificationStatusHistoryElementInt acceptedElementElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom(activeFromAccepted)
                .build();

        Instant activeFromDelivering = activeFromAccepted.plus(Duration.ofDays(1));

        NotificationStatusHistoryElementInt deliveringElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERING)
                .activeFrom(activeFromDelivering)
                .build();

        List<NotificationStatusHistoryElementInt> notificationStatusHistoryElements = new ArrayList<>(List.of(inValidationElement, acceptedElementElement, deliveringElement));

        Mockito.when(
                statusUtils.getStatusHistory(Mockito.anySet() ,Mockito.anyInt(), Mockito.any(Instant.class))
        ).thenReturn(notificationStatusHistoryElements);

        Mockito.when(
                statusUtils.getCurrentStatus( Mockito.anyList() )
        ).thenReturn(currentStatus);

        //WHEN
        NotificationHistoryResponse notificationHistoryResponse = timeLineService.getTimelineAndStatusHistory(iun, numberOfRecipients1, notificationCreatedAt);

        //THEN

        //Viene verificato che il numero di elementi restituiti sia 2, dunque che sia stato eliminato l'elemento con category "IN VALIDATION"
        Assertions.assertEquals(2 , notificationHistoryResponse.getNotificationStatusHistory().size());

        NotificationStatusHistoryElement firstElement = notificationHistoryResponse.getNotificationStatusHistory().get(0);
        Assertions.assertEquals(acceptedElementElement.getStatus(), NotificationStatusInt.valueOf(firstElement.getStatus().getValue()) );
        Assertions.assertEquals(inValidationElement.getActiveFrom(), firstElement.getActiveFrom());

        NotificationStatusHistoryElement secondElement = notificationHistoryResponse.getNotificationStatusHistory().get(1);
        Assertions.assertEquals(deliveringElement.getStatus(), NotificationStatusInt.valueOf(secondElement.getStatus().getValue()));
        Assertions.assertEquals(deliveringElement.getActiveFrom(), secondElement.getActiveFrom());

        //Verifica timeline
        List<TimelineElementInternal> timelineElementList = new ArrayList<>(setTimelineElement);

        Assertions.assertEquals(timelineElementList.size() , notificationHistoryResponse.getTimeline().size());

        var firstElementReturned = notificationHistoryResponse.getTimeline().get(0);

        Assertions.assertEquals( notificationHistoryResponse.getNotificationStatus(), NotificationStatus.valueOf(currentStatus.getValue()) );
        Assertions.assertEquals( elementInternalProg.getElementId(), firstElementReturned.getElementId() );

    }

    @Test
    void getTimelineWithoutDiagnosticElements() {
        //GIVEN
        String iun = "iun";
        int numberOfRecipients1 = 1;
        Instant notificationCreatedAt = Instant.now();
        NotificationStatusInt currentStatus = NotificationStatusInt.DELIVERING;

        String elementId1 = "elementId1";
        Set<TimelineElementInternal> setTimelineElement = new HashSet<>();
        Instant t = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        TimelineElementInternal elementValidatedF24 = getValidatedF24TimelineElement(iun, elementId1+"VALIDATED_F24");
        setTimelineElement.add(elementValidatedF24);
        TimelineElementInternal elementInternalFeedback = getSendPaperFeedbackTimelineElement(iun, elementId1+"FEEDBACK", t);
        setTimelineElement.add(elementInternalFeedback);
        TimelineElementInternal elementInternalProg = getSendPaperProgressTimelineElement(iun, elementId1+"PROGRESS", t);
        setTimelineElement.add(elementInternalProg);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(setTimelineElement);

        Instant activeFromInValidation = Instant.now();

        NotificationStatusHistoryElementInt inValidationElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.IN_VALIDATION)
                .activeFrom(activeFromInValidation)
                .build();

        Instant activeFromAccepted = activeFromInValidation.plus(Duration.ofDays(1));

        NotificationStatusHistoryElementInt acceptedElementElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.ACCEPTED)
                .activeFrom(activeFromAccepted)
                .build();

        Instant activeFromDelivering = activeFromAccepted.plus(Duration.ofDays(1));

        NotificationStatusHistoryElementInt deliveringElement = NotificationStatusHistoryElementInt.builder()
                .status(NotificationStatusInt.DELIVERING)
                .activeFrom(activeFromDelivering)
                .build();

        List<NotificationStatusHistoryElementInt> notificationStatusHistoryElements = new ArrayList<>(List.of(inValidationElement, acceptedElementElement, deliveringElement));

        Mockito.when(
                statusUtils.getStatusHistory(Mockito.anySet() ,Mockito.anyInt(), Mockito.any(Instant.class))
        ).thenReturn(notificationStatusHistoryElements);

        Mockito.when(
                statusUtils.getCurrentStatus( Mockito.anyList() )
        ).thenReturn(currentStatus);

        //WHEN
        NotificationHistoryResponse notificationHistoryResponse = timeLineService.getTimelineAndStatusHistory(iun, numberOfRecipients1, notificationCreatedAt);

        //THEN

        //Viene verificato che il numero di elementi restituiti sia 2, dunque che sia stato eliminato l'elemento con category "IN VALIDATION"
        Assertions.assertEquals(2 , notificationHistoryResponse.getNotificationStatusHistory().size());

        NotificationStatusHistoryElement firstElement = notificationHistoryResponse.getNotificationStatusHistory().get(0);
        Assertions.assertEquals(acceptedElementElement.getStatus(), NotificationStatusInt.valueOf(firstElement.getStatus().getValue()) );
        Assertions.assertEquals(inValidationElement.getActiveFrom(), firstElement.getActiveFrom());

        NotificationStatusHistoryElement secondElement = notificationHistoryResponse.getNotificationStatusHistory().get(1);
        Assertions.assertEquals(deliveringElement.getStatus(), NotificationStatusInt.valueOf(secondElement.getStatus().getValue()));
        Assertions.assertEquals(deliveringElement.getActiveFrom(), secondElement.getActiveFrom());

        //Verifica timeline
        List<TimelineElementV23> timelineElementList = notificationHistoryResponse.getTimeline();

        //Mi aspetto che sia rimosso l'elemento di timeline di diagnostica. (Con category VALIDATE_REQUEST_F24)
        Assertions.assertEquals(2, timelineElementList.size());

        var firstElementReturned = timelineElementList.get(0);
        var secondElementReturned = timelineElementList.get(1);

        Assertions.assertEquals( notificationHistoryResponse.getNotificationStatus(), NotificationStatus.valueOf(currentStatus.getValue()) );
        Assertions.assertEquals( elementInternalProg.getElementId(), firstElementReturned.getElementId() );
        Assertions.assertEquals( elementInternalFeedback.getElementId(), secondElementReturned.getElementId());
        Assertions.assertFalse(timelineElementContainsElementId(timelineElementList, elementId1+"VALIDATED_F24" ) );

    }

    private boolean timelineElementContainsElementId(List<TimelineElementV23> timelineElements, String elementId) {
        return timelineElements.stream()
                .anyMatch(timelineElementV23 -> timelineElementV23.getElementId().equalsIgnoreCase(elementId));
    }

    @Test
    void getSchedulingAnalogDateOKTest() {
        final String iun = "iun1";
        final String recipientId = "cxId";

        String timelineElementIdExpected = TimelineEventId.PROBABLE_SCHEDULING_ANALOG_DATE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .build());

        TimelineElementInternal timelineElementExpected = TimelineElementInternal.builder()
                .elementId(timelineElementIdExpected)
                .timestamp(Instant.now())
                .category(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE)
                .details(ProbableDateAnalogWorkflowDetailsInt.builder()
                        .schedulingAnalogDate(Instant.now())
                        .recIndex(0)
                        .build())
                .build();

        Mockito.when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(NotificationInt.builder()
                        .recipients(List.of(NotificationRecipientInt.builder()
                                .internalId(recipientId)
                                .build()))
                        .build()));

        Mockito.when(timelineDao.getTimeline(iun))
                .thenReturn(Set.of(timelineElementExpected));

        Mockito.when(confidentialInformationService.getTimelineElementConfidentialInformation(iun, timelineElementIdExpected))
                .thenReturn(Optional.empty());

        ProbableSchedulingAnalogDateResponse schedulingAnalogDateActual = timeLineService.getSchedulingAnalogDate(iun, recipientId).block();

        assertThat(schedulingAnalogDateActual.getSchedulingAnalogDate())
                .isEqualTo(((ProbableDateAnalogWorkflowDetailsInt) timelineElementExpected.getDetails()).getSchedulingAnalogDate());

        assertThat(schedulingAnalogDateActual.getIun()).isEqualTo(iun);
        assertThat(schedulingAnalogDateActual.getRecIndex()).isZero();


    }

    @Test
    void getSchedulingAnalogDateNotFoundTest() {
        final String iun = "iun1";
        final String recipientId = "cxId";

        Mockito.when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(NotificationInt.builder()
                        .recipients(List.of(NotificationRecipientInt.builder()
                                .internalId(recipientId)
                                .build()))
                        .build()));

        Mockito.when(timelineDao.getTimeline(iun))
                .thenReturn(Set.of());


        Executable executable = () -> timeLineService.getSchedulingAnalogDate(iun, recipientId).block();
        Assertions.assertThrows(PnNotFoundException.class, executable);

    }


    @Test
    void retrieveAndIncrementCounterForTimelineEventTest() {
        final String timelineid = "iun1";
        TimelineCounterEntity timelineCounterEntity = new TimelineCounterEntity();
        timelineCounterEntity.setTimelineElementId(timelineid);
        timelineCounterEntity.setCounter(5L);

        Mockito.when(timelineCounterDao.getCounter(timelineid))
                .thenReturn(timelineCounterEntity);


        Long r = timeLineService.retrieveAndIncrementCounterForTimelineEvent(timelineid);
        Assertions.assertNotNull(r);
        Assertions.assertEquals(5L, r);
    }
    
    private TimelineElementInternal getSpecificElementFromList(List<TimelineElementInternal> listElement, String timelineId){
        for (TimelineElementInternal element : listElement){
            if(element.getElementId().equals(timelineId)){
                return element;
            }
        }
        return null;
    }
    
    private TimelineElementInternal getSendDigitalTimelineElement(String iun, String timelineId) {
        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .recIndex(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(timelineId)
                .iun(iun)
                .details( details )
                .build();
    }

    private Set<TimelineElementInternal> getSendPaperDetailsList(String iun,  String elementId){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        TimelineElementInternal timelineElementInternal = getSendPaperDetailsTimelineElement(iun, elementId);
        timelineElementList.add(timelineElementInternal);
        return new HashSet<>(timelineElementList);
    }

    private TimelineElementInternal getSendPaperDetailsTimelineElement(String iun, String elementId) {
         SendAnalogDetailsInt details =  SendAnalogDetailsInt.builder()
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .relatedRequestId("abc")
                 .analogCost(100)
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .elementId(elementId)
                .iun(iun)
                .details( details )
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE )
                .build();
    }

    private TimelineElementInternal getAarGenerationTimelineElement(String iun, String elementId) {
        AarGenerationDetailsInt details =  AarGenerationDetailsInt.builder()
                .recIndex(0)
                .generatedAarUrl("url")
                .numberOfPages(1)
                .build();
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .category(TimelineElementCategoryInt.AAR_GENERATION)
                .iun(iun)
                .details( details )
                .build();
    }

    private TimelineElementInternal getSendPaperProgressTimelineElement(String iun, String elementId, Instant timestamp) {
        SendAnalogProgressDetailsInt details =  SendAnalogProgressDetailsInt.builder()
                .recIndex(0)
                .deliveryDetailCode("CON080")
                .notificationDate(timestamp)
                .build();
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .timestamp(timestamp)
                .details( details )
                .build();
    }
    
    private TimelineElementInternal getSendPaperFeedbackTimelineElement(String iun, String elementId, Instant timestamp) {
         SendAnalogFeedbackDetailsInt details =  SendAnalogFeedbackDetailsInt.builder()
                 .notificationDate(timestamp)
                .newAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .timestamp(timestamp)
                .details( details )
                .build();
    }

    private TimelineElementInternal getValidatedF24TimelineElement(String iun, String elementId) {
        ValidatedF24DetailInt detail = ValidatedF24DetailInt.builder().build();

        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .category(TimelineElementCategoryInt.VALIDATE_F24_REQUEST)
                .timestamp(Instant.now())
                .details(detail)
                .build();
    }
    
    private TimelineElementInternal getScheduleAnalogWorkflowTimelineElement(String iun, String timelineId) {
        ScheduleAnalogWorkflowDetailsInt details = ScheduleAnalogWorkflowDetailsInt.builder()
                .recIndex(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(timelineId)
                .iun(iun)
                .details( details )
                .build();
    }
    
    private NotificationInt getNotification(String iun) {
        return NotificationInt.builder()
                .iun(iun)
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }
}