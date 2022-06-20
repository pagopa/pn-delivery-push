package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;
    private final StatusUtils statusUtils;
    private final ConfidentialInformationService confidentialInformationService;
    private final StatusService statusService;
    private final SchedulerService schedulerService;
    
    public TimeLineServiceImpl(TimelineDao timelineDao,
                               StatusUtils statusUtils,
                               StatusService statusService, 
                               ConfidentialInformationService confidentialInformationService,
                               SchedulerService schedulerService) {
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
        this.confidentialInformationService = confidentialInformationService;
        this.statusService = statusService;
        this.schedulerService = schedulerService;
    }

    @Override
    public void addTimelineElement(TimelineElementInternal dto, NotificationInt notification) {
        log.info("addTimelineElement - IUN={} and timelineId={}", dto.getIun(), dto.getElementId());

        if (notification != null) {
            Set<TimelineElementInternal> currentTimeline = getTimeline(dto.getIun());
            StatusService.NotificationStatusUpdate notificationStatuses = statusService.checkAndUpdateStatus(dto, currentTimeline, notification);
            confidentialInformationService.saveTimelineConfidentialInformation(dto);
            timelineDao.addTimelineElement(dto);
            // genero un messagio per l'aggiunta in sqs in modo da salvarlo in maniera asincrona
            schedulerService.scheduleWebhookEvent(notification.getSender().getPaId(), dto.getIun(), dto.getElementId(),
                    dto.getTimestamp(), notificationStatuses.getOldStatus().getValue(), notificationStatuses.getNewStatus().getValue(), dto.getCategory().getValue());
        } else {
            log.error("Try to update Timeline and Status for non existing iun={}", dto.getIun());
            throw new PnInternalException("Try to update Timeline and Status for non existing iun " + dto.getIun());
        }
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN={} and timelineId={}", iun, timelineId);
        //TODO Valutare se possibile passare la category della timeline richiesta e in base verificare se sono presenti informazioni confidenziali,
        // dunque se effettuare la richiesta a data-vault

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineDao.getTimelineElement(iun, timelineId);
        if(timelineElementInternalOpt.isPresent()){
            TimelineElementInternal timelineElementInt = timelineElementInternalOpt.get();
            
            confidentialInformationService.getTimelineElementConfidentialInformation(iun, timelineId).ifPresent(
                    confidentialDto -> enrichTimelineElementWithConfidentialInformation(timelineElementInt, confidentialDto)
            );
            
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
            if (address == null)
            {
                address = DigitalAddress.builder().build();
                details.setDigitalAddress(address);
            }

            address.setAddress(confidentialDto.getDigitalAddress());
        }
    }

    @Override
    public <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("GetTimelineElement - IUN={} and timelineId={}", iun, timelineId);

        Optional<TimelineElementInternal> row = getTimelineElement(iun, timelineId);
        
        if(row.isPresent()){
            T details = SmartMapper.mapToClass(row.get().getDetails(), timelineDetailsClass);
            return Optional.of(details);
        }
        
        return Optional.empty();
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun) {
        log.debug("GetTimeline - iun={} ", iun);
        Set<TimelineElementInternal> setTimelineElements =  this.timelineDao.getTimeline(iun);

        Optional<Map<String, ConfidentialTimelineElementDtoInt>> mapConfOtp;
        mapConfOtp = confidentialInformationService.getTimelineConfidentialInformation(iun);

        if(mapConfOtp.isPresent()){
            Map<String, ConfidentialTimelineElementDtoInt> mapConf = mapConfOtp.get();
            
            setTimelineElements.forEach(
                    timelineElementInt -> {
                        ConfidentialTimelineElementDtoInt dtoInt = mapConf.get(timelineElementInt.getElementId());
                        if(dtoInt != null){
                            enrichTimelineElementWithConfidentialInformation(timelineElementInt, dtoInt);
                        }
                    }
            );
        }
        
        return setTimelineElements;
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("getTimelineAndStatusHistory Start - iun={} ", iun);
        
        Set<TimelineElementInternal> timelineElements = getTimeline(iun);
        
        List<NotificationStatusHistoryElement> statusHistory = statusUtils
                .getStatusHistory( timelineElements, numberOfRecipients, createdAt );

        removeNotToBeReturnedElements(statusHistory);

        NotificationStatus currentStatus = statusUtils.getCurrentStatus( statusHistory );
        
        log.debug("getTimelineAndStatusHistory Ok - iun={} ", iun);

        return createResponse(timelineElements, statusHistory, currentStatus);
    }

    private void removeNotToBeReturnedElements(List<NotificationStatusHistoryElement> statusHistory) {
        
        //Viene eliminato l'elemento InValidation dalla response
        Optional<Instant> inValidationStatusActiveFromOpt = Optional.empty();
        
        for(NotificationStatusHistoryElement element : statusHistory){
            
            if(NotificationStatus.IN_VALIDATION.equals( element.getStatus() )){
                inValidationStatusActiveFromOpt = Optional.of(element.getActiveFrom());
                statusHistory.remove(element);
                break;
            }
        }
        
        if( inValidationStatusActiveFromOpt.isPresent() ){
            
            //Viene sostituito il campo ActiveFrom dell'elemento ACCEPTED con quella dell'elemento eliminato IN_VALIDATION
            Instant inValidationStatusActiveFrom = inValidationStatusActiveFromOpt.get();

            statusHistory.stream()
                    .filter(
                            statusHistoryElement -> NotificationStatus.ACCEPTED.equals( statusHistoryElement.getStatus() )
                    ).findFirst()
                    .ifPresent(
                            el -> el.setActiveFrom(inValidationStatusActiveFrom)
                    );
        }
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
