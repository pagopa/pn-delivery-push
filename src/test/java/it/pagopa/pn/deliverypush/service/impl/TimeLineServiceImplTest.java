package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.StatusService;
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

        TimelineElementInternal newElement = getAarGenerationTimelineElement(iun, elementId);

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
        Set<TimelineElementInternal> retrievedElements = timeLineService.getTimeline(iun);

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
        NotificationStatus currentStatus = NotificationStatus.DELIVERING;

        String elementId1 = "elementId1";
        Set<TimelineElementInternal> setTimelineElement = getSendPaperDetailsList(iun, elementId1);
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
        
        SendAnalogDetailsInt details = (SendAnalogDetailsInt) elementInt.getDetails();
        Assertions.assertEquals( firstElementReturned.getDetails().getRecIndex(), details.getRecIndex());
        Assertions.assertEquals( firstElementReturned.getDetails().getPhysicalAddress().getAddress(), details.getPhysicalAddress().getAddress() );

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
                .investigation(true)
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .details( details )
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
    
    private TimelineElementInternal getSendPaperFeedbackTimelineElement(String iun, String elementId) {
         SendAnalogFeedbackDetailsInt details =  SendAnalogFeedbackDetailsInt.builder()
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
                .details( details )
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