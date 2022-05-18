package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;
    private final StatusUtils statusUtils;
    private final ConfidentialInformationService confidentialInformationService;
    
    public TimeLineServiceImpl(TimelineDao timelineDao, StatusUtils statusUtils, ConfidentialInformationService confidentialInformationService) {
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
        this.confidentialInformationService = confidentialInformationService;
    }

    @Override
    public void addTimelineElement(TimelineElementInternal element) {
        log.debug("addTimelineElement - IUN {} and timelineId {}", element.getIun(), element.getElementId());
        confidentialInformationService.saveTimelineConfidentialInformation(element);
        timelineDao.addTimelineElement(element);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN {} and timelineId {}", iun, timelineId);
        //TODO Valutare se possibile passare la category della timeline richiesta e in base verificare se sono presenti informazioni confidenziali, dunque se effettuare la richiesta a data-vault

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineDao.getTimelineElement(iun, timelineId);
        if(timelineElementInternalOpt.isPresent()){
            TimelineElementInternal timelineElementInt = timelineElementInternalOpt.get();
            ConfidentialTimelineElementDtoInt confidentialDto = confidentialInformationService.getTimelineConfidentialInformation(iun, timelineId);
            
            enrichTimelineElementWithConfidentialInformation(timelineElementInt, confidentialDto);
            
            return Optional.of(timelineElementInt);
        }
        
        return Optional.empty();
    }

    private void enrichTimelineElementWithConfidentialInformation(TimelineElementInternal timelineElementInt,
                                                                  ConfidentialTimelineElementDtoInt confidentialDto) {
        TimelineElementDetails details = timelineElementInt.getDetails();

        details.setPhysicalAddress(confidentialDto.getPhysicalAddress());
        details.setNewAddress(confidentialDto.getNewPhysicalAddress());
        
        if(confidentialDto.getDigitalAddress() != null){
            DigitalAddress address = details.getDigitalAddress();
            address.setAddress(confidentialDto.getDigitalAddress());
        }
    }

    @Override
    public <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("GetTimelineElement - IUN {} and timelineId {}", iun, timelineId);

        Optional<TimelineElementInternal> row = this.timelineDao.getTimelineElement(iun, timelineId);
        
        if(row.isPresent()){
            T details = SmartMapper.mapToClass(row.get().getDetails(), timelineDetailsClass);
            return Optional.of(details);
        }
        
        return Optional.empty();
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

        List<TimelineElement> timelineList = timelineElements.stream()
                .map(internalElement -> SmartMapper.mapToClass(internalElement, TimelineElement.class))
                .collect(Collectors.toList());
        
        return NotificationHistoryResponse.builder()
                .timeline(timelineList)
                .notificationStatusHistory(statusHistory)
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
