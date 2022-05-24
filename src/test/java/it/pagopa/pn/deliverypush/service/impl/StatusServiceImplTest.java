package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

class StatusServiceImplTest {
    private PnDeliveryClient pnDeliveryClient;
    private StatusUtils statusUtils;
    
    private StatusService statusService;
    
    @BeforeEach
    void setup() {
        pnDeliveryClient = Mockito.mock( PnDeliveryClient.class );
        statusUtils = Mockito.mock( StatusUtils.class );

        statusService = new StatusServiceImpl(pnDeliveryClient, statusUtils);
    }

    @Test
    void updateStatus() {
        // GIVEN
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";
        
        List<NotificationStatusHistoryElement> firstListReturn = new ArrayList<>();
        NotificationStatusHistoryElement element = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.DELIVERING)
                .build();
        firstListReturn.add(element);

        NotificationStatusHistoryElement element2 = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.ACCEPTED)
                .build();
        List<NotificationStatusHistoryElement> secondListReturn = new ArrayList<>(firstListReturn);
        secondListReturn.add(element2);

        Mockito.when(statusUtils.getStatusHistory(Mockito.any(), Mockito.anyInt(), Mockito.any() ))
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn)
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn);

        Mockito.when(pnDeliveryClient.updateStatus(Mockito.any(RequestUpdateStatusDto.class))).thenReturn(ResponseEntity.ok().body(null));
                
        NotificationInt notification = getNotification(iun);
        
        
        String id1 = "sender_ack";
        TimelineElementInternal dto = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(SmartMapper.mapToClass(new NotificationRequestAccepted(), TimelineElementDetails.class))
                .timestamp(Instant.now())
                .build();

        List<TimelineElementInternal> timelineElementList  =  getListTimelineElementInternal(iun);
        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);
        
        //WHEN
        statusService.checkAndUpdateStatus(dto, hashSet, notification);
        
        //THEN
        Mockito.verify(pnDeliveryClient).updateStatus(Mockito.any(RequestUpdateStatusDto.class));
    }

    @Test
    void notUpdateStatus() {
        // GIVEN
        String iun = "202109-eb10750e-e876-4a5a-8762-c4348d679d35";

        List<NotificationStatusHistoryElement> firstListReturn = new ArrayList<>();
        NotificationStatusHistoryElement element = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.ACCEPTED)
                .build();
        firstListReturn.add(element);

        NotificationStatusHistoryElement element2 = NotificationStatusHistoryElement.builder()
                .status(NotificationStatus.ACCEPTED)
                .build();
        List<NotificationStatusHistoryElement> secondListReturn = new ArrayList<>(firstListReturn);
        secondListReturn.add(element2);

        Mockito.when(statusUtils.getStatusHistory(Mockito.any(), Mockito.anyInt(), Mockito.any() ))
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn)
                .thenReturn(firstListReturn)
                .thenReturn(secondListReturn);

        Mockito.when(pnDeliveryClient.updateStatus(Mockito.any(RequestUpdateStatusDto.class))).thenReturn(ResponseEntity.ok().body(null));

        NotificationInt notification = getNotification(iun);


        String id1 = "sender_ack";
        TimelineElementInternal dto = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .elementId(id1)
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .details(SmartMapper.mapToClass(new NotificationRequestAccepted(), TimelineElementDetails.class))
                .timestamp(Instant.now())
                .build();

        List<TimelineElementInternal> timelineElementList  =  getListTimelineElementInternal(iun);
        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);

        //WHEN
        statusService.checkAndUpdateStatus(dto, hashSet, notification);

        //THEN
        Mockito.verify(pnDeliveryClient, Mockito.never()).updateStatus(Mockito.any(RequestUpdateStatusDto.class));
    }
    
    private List<TimelineElementInternal> getListTimelineElementInternal(String iun){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
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
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();

        timelineElementList.add(timelineElementInternal);

        return timelineElementList;
    }

    private TimelineElementInternal getTimelineElement(String iun) {
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