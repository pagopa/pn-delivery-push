package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeLineServiceImplTest {
    private TimelineDao timelineDao;
    private StatusUtils statusUtils;
    private TimeLineServiceImpl timeLineService;
    private StatusService statusService;
    private ConfidentialInformationService confidentialInformationService;
    private SchedulerService schedulerService;

    @BeforeEach
    void setup() {
        timelineDao = Mockito.mock( TimelineDao.class );
        statusUtils = Mockito.mock( StatusUtils.class );
        statusService = Mockito.mock( StatusService.class );
        confidentialInformationService = Mockito.mock( ConfidentialInformationService.class );
        schedulerService = Mockito.mock(SchedulerService.class);
        
        timeLineService = new TimeLineServiceImpl(timelineDao , statusUtils, statusService, confidentialInformationService, schedulerService);
    }

    @Test
    void addTimelineElement(){
        //GIVEN
        String iun = "iun";
        String elementId = "elementId";

        NotificationInt notification = getNotification(iun);
        StatusService.NotificationStatusUpdate notificationStatuses = new StatusService.NotificationStatusUpdate(NotificationStatus.ACCEPTED, NotificationStatus.ACCEPTED);
        Mockito.when(statusService.checkAndUpdateStatus(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(notificationStatuses);
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        String elementId2 = "elementId2";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId2);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(setTimelineElement);

        TimelineElementInternal newElement = getSendPaperFeedbackTimelineElement(iun, elementId);
        newElement.setCategory(TimelineElementCategory.AAR_GENERATION);

        //WHEN
        timeLineService.addTimelineElement(newElement, notification);
        
        //THEN
        Mockito.verify(timelineDao).addTimelineElement(newElement);
        Mockito.verify(statusService).checkAndUpdateStatus(newElement, setTimelineElement, notification);
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

        TimelineElementInternal newElement = getSendPaperFeedbackTimelineElement(iun, elementId);

        Mockito.doThrow(new PnInternalException("error")).when(statusService).checkAndUpdateStatus(Mockito.any(TimelineElementInternal.class), Mockito.anySet(), Mockito.any(NotificationInt.class));

        // WHEN
        assertThrows(PnInternalException.class, () -> {
            timeLineService.addTimelineElement(newElement, notification);
        });
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
        Assertions.assertEquals(retrievedElement.get().getDetails().getDigitalAddress().getType(), daoElement.getDetails().getDigitalAddress().getType());
        Assertions.assertEquals(retrievedElement.get().getDetails().getDigitalAddress().getAddress(), confidentialTimelineElementDtoInt.getDigitalAddress());
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
        Optional<SendDigitalDetails> detailsOpt = timeLineService.getTimelineElementDetails(iun, timelineId, SendDigitalDetails.class);

        //THEN
        Assertions.assertTrue(detailsOpt.isPresent());
        Assertions.assertEquals(detailsOpt.get().getRecIndex(), daoElement.getDetails().getRecIndex());
        Assertions.assertEquals(detailsOpt.get().getDigitalAddress().getType(), daoElement.getDetails().getDigitalAddress().getType());
        Assertions.assertEquals(detailsOpt.get().getDigitalAddress().getAddress(), confidentialTimelineElementDtoInt.getDigitalAddress());
    }

    @Test
    void getTimelineElementDetailsEmpty(){
        //GIVEN
        String iun = "iun";
        String timelineId = "idTimeline";

        Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        //WHEN
        Optional<SendDigitalDetails> detailsOpt = timeLineService.getTimelineElementDetails(iun, timelineId, SendDigitalDetails.class);

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
        Assertions.assertEquals(retrievedElement.get().getDetails().getRecIndex(), daoElement.getDetails().getRecIndex());
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
        TimelineElementInternal sendPaperFeedbackConfInf = getSendPaperFeedbackTimelineElement(iun, timelineId3);

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
                        PhysicalAddress.builder()
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
        Set<TimelineElementInternal> retrievedElements = timeLineService.getTimeline(iun);

        //THEN
        Assertions.assertFalse(retrievedElements.isEmpty());

        List<TimelineElementInternal> listElement = new ArrayList<>(retrievedElements);

        TimelineElementInternal retrievedScheduleAnalog = getSpecificElementFromList(listElement, scheduleAnalogNoConfInf.getElementId());
        Assertions.assertEquals(retrievedScheduleAnalog , scheduleAnalogNoConfInf);

        TimelineElementInternal retrievedSendDigital = getSpecificElementFromList(listElement, sendDigitalConfInf.getElementId());
        Assertions.assertNotNull(retrievedSendDigital);
        Assertions.assertEquals(retrievedSendDigital.getDetails().getRecIndex() , sendDigitalConfInf.getDetails().getRecIndex());
        Assertions.assertEquals(retrievedSendDigital.getDetails().getDigitalAddress().getAddress() , confInfDigital.getDigitalAddress());

        TimelineElementInternal retrievedSendPaperFeedback = getSpecificElementFromList(listElement, sendPaperFeedbackConfInf.getElementId());
        Assertions.assertNotNull(retrievedSendPaperFeedback);
        Assertions.assertEquals(retrievedSendPaperFeedback.getDetails().getRecIndex() , sendPaperFeedbackConfInf.getDetails().getRecIndex());
        Assertions.assertEquals(retrievedSendPaperFeedback.getDetails().getPhysicalAddress() , confInfPhysical.getPhysicalAddress());
    }

    @Test
    void getTimelineAndStatusHistory() {
        //GIVEN
        String iun = "iun";
        int numberOfRecipients1 = 1;
        Instant notificationCreatedAt = Instant.now();
        NotificationStatus currentStatus = NotificationStatus.DELIVERING;

        String elementId2 = "elementId2";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId2);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(setTimelineElement);

        Instant activeFromInValidation = Instant.now();
        
        NotificationStatusHistoryElement inValidationElement = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.IN_VALIDATION)
                .activeFrom(activeFromInValidation)
                .build();

        Instant activeFromAccepted = activeFromInValidation.plus(Duration.ofDays(1));

        NotificationStatusHistoryElement acceptedElementElement = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.ACCEPTED)
                .activeFrom(activeFromAccepted)
                .build();

        Instant activeFromDelivering = activeFromAccepted.plus(Duration.ofDays(1));

        NotificationStatusHistoryElement deliveringElement = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.DELIVERING)
                .activeFrom(activeFromDelivering)
                .build();
        
        List<NotificationStatusHistoryElement> notificationStatusHistoryElements = new ArrayList<>(List.of(inValidationElement, acceptedElementElement, deliveringElement));

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
        Assertions.assertEquals(acceptedElementElement.getStatus(), firstElement.getStatus());
        Assertions.assertEquals(inValidationElement.getActiveFrom(), firstElement.getActiveFrom());

        NotificationStatusHistoryElement secondElement = notificationHistoryResponse.getNotificationStatusHistory().get(1);
        Assertions.assertEquals(deliveringElement.getStatus(), secondElement.getStatus());
        Assertions.assertEquals(deliveringElement.getActiveFrom(), secondElement.getActiveFrom());
        
        //Verifica timeline 
        List<TimelineElementInternal> timelineElementList = new ArrayList<>(setTimelineElement);
        TimelineElementInternal elementInt = timelineElementList.get(0);

        Assertions.assertEquals(timelineElementList.size() , notificationHistoryResponse.getTimeline().size());

        TimelineElement firstElementReturned = notificationHistoryResponse.getTimeline().get(0);
        
        Assertions.assertEquals( notificationHistoryResponse.getNotificationStatus(), currentStatus );
        Assertions.assertEquals( elementInt.getElementId(), firstElementReturned.getElementId() );
        Assertions.assertEquals( elementInt.getDetails().getRecIndex(), firstElementReturned.getDetails().getRecIndex() );
        Assertions.assertEquals( elementInt.getDetails().getPhysicalAddress().getAddress(), firstElementReturned.getDetails().getPhysicalAddress().getAddress() );

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
        SendDigitalDetails details = SendDigitalDetails.builder()
                .digitalAddressSource(DigitalAddressSource.SPECIAL)
                .digitalAddress(
                        DigitalAddress.builder()
                                .type("PEC")
                                .build()
                )
                .recIndex(0)
                .build();
        return TimelineElementInternal.timelineInternalBuilder()
                .elementId(timelineId)
                .iun(iun)
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();
    }

    private Set<TimelineElementInternal> getSendPaperDetailsList(String iun,  String elementId){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        TimelineElementInternal timelineElementInternal = getSendPaperDetailsTimelineElement(iun, elementId);
        timelineElementList.add(timelineElementInternal);
        return new HashSet<>(timelineElementList);
    }

    private TimelineElementInternal getSendPaperDetailsTimelineElement(String iun, String elementId) {
        SendPaperDetails details = SendPaperDetails.builder()
                .physicalAddress(
                        PhysicalAddress.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .investigation(true)
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.timelineInternalBuilder()
                .elementId(elementId)
                .iun(iun)
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();
    }

    private TimelineElementInternal getSendPaperFeedbackTimelineElement(String iun, String elementId) {
        SendPaperFeedbackDetails details = SendPaperFeedbackDetails.builder()
                .newAddress(
                        PhysicalAddress.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.timelineInternalBuilder()
                .elementId(elementId)
                .iun(iun)
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();
    }
    
    private TimelineElementInternal getScheduleAnalogWorkflowTimelineElement(String iun, String timelineId) {
        ScheduleAnalogWorkflow details = ScheduleAnalogWorkflow.builder()
                .recIndex(0)
                .build();
        return TimelineElementInternal.timelineInternalBuilder()
                .elementId(timelineId)
                .iun(iun)
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();
    }
    
    private NotificationInt getNotification(String iun) {
        return NotificationInt.builder()
                .iun(iun)
                .paNotificationId("protocol_01")
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