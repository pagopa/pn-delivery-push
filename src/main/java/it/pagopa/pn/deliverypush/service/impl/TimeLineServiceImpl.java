package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;
    private final StatusUtils statusUtils;
    private final TimelineUtils timelineUtils;
    
    public TimeLineServiceImpl(TimelineDao timelineDao, StatusUtils statusUtils, TimelineUtils timelineUtils) {
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
        this.timelineUtils = timelineUtils;
    }

    @Override
    public void addTimelineElement(TimelineElementInternal element) {
        log.debug("addTimelineElement - IUN {} and timelineId {}", element.getIun(), element.getElementId());
        timelineDao.addTimelineElement(element);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN {} and timelineId {}", iun, timelineId);
        return timelineDao.getTimelineElement(iun, timelineId);
    }

    @Override
    public <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("GetTimelineElement - IUN {} and timelineId {}", iun, timelineId);

        Optional<TimelineElementInternal> row = this.timelineDao.getTimelineElement(iun, timelineId);
        return row.map(el -> timelineDetailsClass.cast(el.getDetails()));
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun) {
        log.debug("GetTimeline - iun {} ", iun);
        return this.timelineDao.getTimeline(iun);
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("getTimelineAndStatusHistory Start - iun {} ", iun);
        
        Set<TimelineElementInternal> timelineElements = this.timelineDao.getTimeline(iun);

        List<NotificationStatusHistoryElement> statusHistory = statusUtils
                .getStatusHistory( timelineElements, numberOfRecipients, createdAt );

        NotificationStatus currentStatus = statusUtils.getCurrentStatus( statusHistory );
        
        log.debug("getTimelineAndStatusHistory Ok - iun {} ", iun);

        return createResponse(timelineElements, statusHistory, currentStatus);
    }

    private NotificationHistoryResponse createResponse(Set<TimelineElementInternal> timelineElements, List<NotificationStatusHistoryElement> statusHistory,
                                                       NotificationStatus currentStatus) {
         new ArrayList<>(timelineElements);
        List<NotificationStatusHistoryElement> historyList = new ArrayList<>(statusHistory);

        List<TimelineElement> timelineList = timelineElements.stream().map(
                element ->  {
                    TimelineElementInternal timelineElement = TimelineElementInternal.timelineInternalBuilder().build();
                    BeanUtils.copyProperties(element, timelineElement,
                            "iun");
                    return timelineElement;
                }
        ).collect(Collectors.toList());
        
        
        return NotificationHistoryResponse.builder()
                .timeline(timelineList)
                .notificationStatusHistory(historyList)
                .notificationStatus(currentStatus)
                .build();
    }

    @Override
    public boolean isPresentTimeLineElement(String iun, Integer recIndex, TimelineEventId timelineEventId) {
        EventId eventId = EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .build();
        return this.timelineDao.getTimelineElement(iun, timelineEventId.buildEventId(eventId)).isPresent();
    }

}
