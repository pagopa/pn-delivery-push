package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeLineServiceImplTest {
    private TimelineDao timelineDao;
    private StatusUtils statusUtils;
    private TimeLineServiceImpl timeLineService;
    private NotificationService notificationService;
    private StatusService statusService;

    @BeforeEach
    void setup() {
        timelineDao = Mockito.mock( TimelineDao.class );
        statusUtils = Mockito.mock( StatusUtils.class );
        notificationService = Mockito.mock( NotificationService.class );
        statusService = Mockito.mock( StatusService.class );

        timeLineService = new TimeLineServiceImpl(timelineDao , statusUtils, notificationService, statusService);
    }

    @Test
    void addTimelineElement(){
        //GIVEN
        String iun = "iun";
        
        NotificationInt notification = getNotification(iun);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        List<TimelineElementInternal> timelineElementList  =  getListTimelineElementInternal(iun);
        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);

        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(hashSet);

        TimelineElementInternal newElement = getTimelineElement(iun);
        
        //WHEN
        timeLineService.addTimelineElement(newElement);
        
        //THEN
        Mockito.verify(timelineDao).addTimelineElement(newElement);
    }

    @Test
    void addTimelineElementError(){
        //GIVEN
        String iun = "iun";

        NotificationInt notification = getNotification(iun);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        List<TimelineElementInternal> timelineElementList  =  getListTimelineElementInternal(iun);
        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);

        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(hashSet);

        TimelineElementInternal newElement = getTimelineElement(iun);

        Mockito.doThrow(new PnInternalException("error")).when(statusService).checkAndUpdateStatus(Mockito.any(TimelineElementInternal.class), Mockito.anySet(), Mockito.any(NotificationInt.class));

        // WHEN
        assertThrows(PnInternalException.class, () -> {
            timeLineService.addTimelineElement(newElement);
        });
    }
    
    @Test
    void getTimelineAndStatusHistory() {
        //GIVEN
        String iun = "iun";
        int numberOfRecipients1 = 1;
        Instant notificationCreatedAt = Instant.now();
        NotificationStatus currentStatus = NotificationStatus.DELIVERING;

        List<TimelineElementInternal> timelineElementList  =  getListTimelineElementInternal(iun);
        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);
        
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(hashSet);

        List<NotificationStatusHistoryElement> notificationStatusHistoryElements = Collections.singletonList(
                NotificationStatusHistoryElement.builder()
                        .status(NotificationStatus.DELIVERING)
                        .activeFrom(Instant.now())
                        .build()
        );
        Mockito.when(
                statusUtils.getStatusHistory(Mockito.anySet() ,Mockito.anyInt(), Mockito.any(Instant.class))
        ).thenReturn(notificationStatusHistoryElements);

        Mockito.when(
                statusUtils.getCurrentStatus( Mockito.anyList() )
        ).thenReturn(currentStatus);
        
        //WHEN
        NotificationHistoryResponse notificationHistoryResponse = timeLineService.getTimelineAndStatusHistory(iun, numberOfRecipients1, notificationCreatedAt);
        
        //THEN
        TimelineElement element = notificationHistoryResponse.getTimeline().get(0);
        TimelineElementInternal elementInt = timelineElementList.get(0);
        
        Assertions.assertEquals( notificationHistoryResponse.getNotificationStatus(), currentStatus );
        Assertions.assertEquals( elementInt.getElementId(), element.getElementId() );
        Assertions.assertEquals( elementInt.getDetails().getRecIndex(), element.getDetails().getRecIndex() );
        Assertions.assertEquals( elementInt.getDetails().getPhysicalAddress().getAddress(), element.getDetails().getPhysicalAddress().getAddress() );

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