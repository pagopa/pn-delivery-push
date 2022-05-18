package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

class TimeLineServiceImplTest {
    private TimelineDao timelineDao;
    private StatusUtils statusUtils;
    private TimeLineServiceImpl timeLineService;
    
    @BeforeEach
    void setup() {
        timelineDao = Mockito.mock( TimelineDao.class );
        statusUtils = Mockito.mock( StatusUtils.class );
        timeLineService = new TimeLineServiceImpl(timelineDao , statusUtils);
    }

    @Test
    void getTimelineAndStatusHistory() {
        String iun = "iun";
        int numberOfRecipients1 = 1;
        Instant notificationCreatedAt = Instant.now();
        NotificationStatus currentStatus = NotificationStatus.DELIVERING;

        List<TimelineElementInternal> timelineElementList  =  getTimelineElementInternal(iun);
        List<NotificationStatusHistoryElement> notificationStatusHistoryElements = Collections.singletonList(
                NotificationStatusHistoryElement.builder()
                        .status(NotificationStatus.DELIVERING)
                        .activeFrom(Instant.now())
                        .build()
        );
        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);
        
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(hashSet);

        Mockito.when(
                statusUtils.getStatusHistory(Mockito.anySet() ,Mockito.anyInt(), Mockito.any(Instant.class))
        ).thenReturn(notificationStatusHistoryElements);

        Mockito.when(
                statusUtils.getCurrentStatus( Mockito.anyList() )
        ).thenReturn(currentStatus);
                
        NotificationHistoryResponse notificationHistoryResponse = timeLineService.getTimelineAndStatusHistory(iun, numberOfRecipients1, notificationCreatedAt);

        TimelineElement element = notificationHistoryResponse.getTimeline().get(0);
        TimelineElementInternal elementInt = timelineElementList.get(0);
        
        Assertions.assertEquals( notificationHistoryResponse.getNotificationStatus(), currentStatus );
        Assertions.assertEquals( elementInt.getElementId(), element.getElementId() );
        Assertions.assertEquals( elementInt.getDetails().getRecIndex(), element.getDetails().getRecIndex() );
        Assertions.assertEquals( elementInt.getDetails().getNewAddress().getAddress(), element.getDetails().getNewAddress().getAddress() );

    }
    
    private List<TimelineElementInternal> getTimelineElementInternal(String iun){
        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
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
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();
        
        timelineElementList.add(timelineElementInternal);
 
        return timelineElementList;
    }
}