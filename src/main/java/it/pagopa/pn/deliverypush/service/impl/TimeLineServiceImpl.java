package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationStatusHistoryElementMapper;
import it.pagopa.pn.deliverypush.service.mapper.TimelineElementMapper;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.extern.slf4j.Slf4j;
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
            
            //Vengono salvate le informazioni confidenziali in sicuro, dal momento che successivamente non saranno salvate a DB
            confidentialInformationService.saveTimelineConfidentialInformation(dto);
            
            timelineDao.addTimelineElement(dto);
            // genero un messagio per l'aggiunta in sqs in modo da salvarlo in maniera asincrona
            schedulerService.scheduleWebhookEvent(notification.getSender().getPaId(),
                    dto.getIun(),
                    dto.getElementId(),
                    dto.getTimestamp(),
                    notificationStatuses.getOldStatus().getValue(),
                    notificationStatuses.getNewStatus().getValue(),
                    dto.getCategory().getValue()
            );
        } else {
            log.error("Try to update Timeline and Status for non existing iun={}", dto.getIun());
            throw new PnInternalException("Try to update Timeline and Status for non existing iun " + dto.getIun());
        }
    }
    
    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN={} and timelineId={}", iun, timelineId);

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineDao.getTimelineElement(iun, timelineId);
        if(timelineElementInternalOpt.isPresent()){
            TimelineElementInternal timelineElementInt = timelineElementInternalOpt.get();
            
            confidentialInformationService.getTimelineElementConfidentialInformation(iun, timelineId).ifPresent(
                    confidentialDto -> enrichTimelineElementWithConfidentialInformation(
                            timelineElementInt.getDetails(), confidentialDto
                    )
            );
            
            return Optional.of(timelineElementInt);
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("GetTimelineElement - IUN={} and timelineId={}", iun, timelineId);
        
        Optional<TimelineElementInternal> timelineElementOpt = this.timelineDao.getTimelineElement(iun, timelineId);
        if(timelineElementOpt.isPresent()){
            TimelineElementInternal timelineElement = timelineElementOpt.get();
            
            confidentialInformationService.getTimelineElementConfidentialInformation(iun, timelineId).ifPresent(
                    confidentialDto -> enrichTimelineElementWithConfidentialInformation(
                            timelineElement.getDetails(), confidentialDto
                    )
            );
            
            return Optional.of(timelineDetailsClass.cast(timelineElement.getDetails()));
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
                            enrichTimelineElementWithConfidentialInformation(timelineElementInt.getDetails(), dtoInt);
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
        
        List<NotificationStatusHistoryElementInt> statusHistory = statusUtils
                .getStatusHistory( timelineElements, numberOfRecipients, createdAt );

        removeNotToBeReturnedElements(statusHistory);

        NotificationStatusInt currentStatus = statusUtils.getCurrentStatus( statusHistory );
        
        log.debug("getTimelineAndStatusHistory Ok - iun={} ", iun);

        return createResponse(timelineElements, statusHistory, currentStatus);
    }

    private void removeNotToBeReturnedElements(List<NotificationStatusHistoryElementInt> statusHistory) {
        
        //Viene eliminato l'elemento InValidation dalla response
        Optional<Instant> inValidationStatusActiveFromOpt = Optional.empty();
        
        for(NotificationStatusHistoryElementInt element : statusHistory){
            
            if(NotificationStatusInt.IN_VALIDATION.equals( element.getStatus() )){
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
                            statusHistoryElement -> NotificationStatusInt.ACCEPTED.equals( statusHistoryElement.getStatus() )
                    ).findFirst()
                    .ifPresent(
                            el -> el.setActiveFrom(inValidationStatusActiveFrom)
                    );
        }
    }

    private NotificationHistoryResponse createResponse(Set<TimelineElementInternal> timelineElements, List<NotificationStatusHistoryElementInt> statusHistory,
                                                       NotificationStatusInt currentStatus) {

        List<TimelineElement> timelineList = timelineElements.stream()
                .map(TimelineElementMapper::internalToExternal)
                .collect(Collectors.toList());
        
        return NotificationHistoryResponse.builder()
                .timeline(timelineList)
                .notificationStatusHistory(
                        statusHistory.stream().map(
                                NotificationStatusHistoryElementMapper::internalToExternal
                        ).collect(Collectors.toList())
                )
                .notificationStatus(currentStatus != null ? NotificationStatus.valueOf(currentStatus.getValue()) : null)
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

    public void enrichTimelineElementWithConfidentialInformation(TimelineElementDetailsInt details,
                                                                 ConfidentialTimelineElementDtoInt confidentialDto) {
        if( details instanceof CourtesyAddressRelatedTimelineElement){
            CourtesyDigitalAddressInt address = ((CourtesyAddressRelatedTimelineElement) details).getDigitalAddress();

            if (address == null)
            {
                address = CourtesyDigitalAddressInt.builder().build();
            }

            address = address.toBuilder().address(confidentialDto.getDigitalAddress()).build();
            ((CourtesyAddressRelatedTimelineElement) details).setDigitalAddress(address);
        }

        if( details instanceof DigitalAddressRelatedTimelineElement && confidentialDto.getDigitalAddress() != null){

            LegalDigitalAddressInt address = ((DigitalAddressRelatedTimelineElement) details).getDigitalAddress();

            if (address == null)
            {
                address = LegalDigitalAddressInt.builder().build();
            }

            address = address.toBuilder().address(confidentialDto.getDigitalAddress()).build();
            ((DigitalAddressRelatedTimelineElement) details).setDigitalAddress(address);
        }

        if( details instanceof PhysicalAddressRelatedTimelineElement){
            PhysicalAddressInt physicalAddress = ((PhysicalAddressRelatedTimelineElement) details).getPhysicalAddress();

            if (physicalAddress == null)
            {
                physicalAddress = PhysicalAddressInt.builder().build();
            }

            physicalAddress = physicalAddress.toBuilder()
                    .at(confidentialDto.getPhysicalAddress().getAt())
                    .address(confidentialDto.getPhysicalAddress().getAddress())
                    .municipality(confidentialDto.getPhysicalAddress().getMunicipality())
                    .province(confidentialDto.getPhysicalAddress().getProvince())
                    .addressDetails(confidentialDto.getPhysicalAddress().getAddressDetails())
                    .zip(confidentialDto.getPhysicalAddress().getZip())
                    .municipalityDetails(confidentialDto.getPhysicalAddress().getMunicipalityDetails())
                    .build();
            
            ((PhysicalAddressRelatedTimelineElement) details).setPhysicalAddress(physicalAddress);
        }

        if( details instanceof NewAddressRelatedTimelineElement){
            
            if( confidentialDto.getNewPhysicalAddress() != null ){
                
                PhysicalAddressInt newAddress = ((NewAddressRelatedTimelineElement) details).getNewAddress();

                if (newAddress == null)
                {
                    newAddress = PhysicalAddressInt.builder().build();
                }

                newAddress = newAddress.toBuilder()
                        .at(confidentialDto.getNewPhysicalAddress().getAt())
                        .address(confidentialDto.getNewPhysicalAddress().getAddress())
                        .municipality(confidentialDto.getNewPhysicalAddress().getMunicipality())
                        .province(confidentialDto.getNewPhysicalAddress().getProvince())
                        .addressDetails(confidentialDto.getNewPhysicalAddress().getAddressDetails())
                        .zip(confidentialDto.getNewPhysicalAddress().getZip())
                        .municipalityDetails(confidentialDto.getNewPhysicalAddress().getMunicipalityDetails())
                        .build();

                ((NewAddressRelatedTimelineElement) details).setNewAddress(newAddress);
                
            }
        }
    }


}
