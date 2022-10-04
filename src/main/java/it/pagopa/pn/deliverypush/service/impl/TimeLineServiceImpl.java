package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
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
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.StatusInfoEntity;
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

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED;

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
        log.debug("addTimelineElement - IUN={} and timelineId={}", dto.getIun(), dto.getElementId());
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = getPnAuditLogEvent(dto, auditLogBuilder);
        logEvent.log();

        if (notification != null) {
            try{
                Set<TimelineElementInternal> currentTimeline = getTimeline(dto.getIun(), true);
                StatusService.NotificationStatusUpdate notificationStatuses = statusService.checkAndUpdateStatus(dto, currentTimeline, notification);

                //Vengono salvate le informazioni confidenziali in sicuro, dal momento che successivamente non saranno salvate a DB
                confidentialInformationService.saveTimelineConfidentialInformation(dto);

                //Creo lo statusInfo che poi verrà salvato nell'entity
                StatusInfoEntity statusInfo = buildStatusInfo(notificationStatuses);

                timelineDao.addTimelineElement(dto, statusInfo);
                // genero un messaggio per l'aggiunta in sqs in modo da salvarlo in maniera asincrona
                schedulerService.scheduleWebhookEvent(
                        notification.getSender().getPaId(),
                        dto.getIun(),
                        dto.getElementId(),
                        dto.getTimestamp(),
                        notificationStatuses.getOldStatus().getValue(),
                        notificationStatuses.getNewStatus().getValue(),
                        dto.getCategory().getValue()
                );
                logEvent.generateSuccess().log();
            } catch (Exception ex) {
                logEvent.generateFailure("Exception in addTimelineElement, ex={}", ex).log();
                throw new PnInternalException("Exception in addTimelineElement - iun=" + notification.getIun() + " elementId=" + dto.getElementId(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED, ex);
            }

        } else {
            logEvent.generateFailure("Try to update Timeline and Status for non existing iun={}", dto.getIun());
            throw new PnInternalException("Try to update Timeline and Status for non existing iun " + dto.getIun(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED);
        }

    }

    private PnAuditLogEvent getPnAuditLogEvent(TimelineElementInternal dto, PnAuditLogBuilder auditLogBuilder) {
        String auditLog = String.format(
                "Add timeline element: CATEGORY=%s IUN=%s {DETAILS: %s} TIMELINEID=%s TIMESTAMP=%s",
                dto.getCategory(),
                dto.getIun(),
                dto.getDetails().toLog(),
                dto.getElementId(),
                dto.getTimestamp()
        );

        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_TIMELINE, auditLog)
                .iun(dto.getIun())
                .build();
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN={} and timelineId={}", iun, timelineId);

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineDao.getTimelineElement(iun, timelineId);
        if (timelineElementInternalOpt.isPresent()) {
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
        if (timelineElementOpt.isPresent()) {
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
    public Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired) {
        log.debug("GetTimeline - iun={} ", iun);
        Set<TimelineElementInternal> setTimelineElements = this.timelineDao.getTimeline(iun);

        if (confidentialInfoRequired) {
            Optional<Map<String, ConfidentialTimelineElementDtoInt>> mapConfOtp;
            mapConfOtp = confidentialInformationService.getTimelineConfidentialInformation(iun);
            
            if (mapConfOtp.isPresent()) {
                Map<String, ConfidentialTimelineElementDtoInt> mapConf = mapConfOtp.get();

                setTimelineElements.forEach(
                        timelineElementInt -> {
                            ConfidentialTimelineElementDtoInt dtoInt = mapConf.get(timelineElementInt.getElementId());
                            if (dtoInt != null) {
                                enrichTimelineElementWithConfidentialInformation(timelineElementInt.getDetails(), dtoInt);
                            }
                        }
                );
            }
        }
        return setTimelineElements;
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        log.debug("GetTimeline - iun={} timelineId={}", iun, timelineId);
        Set<TimelineElementInternal> setTimelineElements = this.timelineDao.getTimelineFilteredByElementId(iun, timelineId);

        if (confidentialInfoRequired) {
            Optional<Map<String, ConfidentialTimelineElementDtoInt>> mapConfOtp;
            mapConfOtp = confidentialInformationService.getTimelineConfidentialInformation(iun);

            if (mapConfOtp.isPresent()) {
                Map<String, ConfidentialTimelineElementDtoInt> mapConf = mapConfOtp.get();

                setTimelineElements.forEach(
                        timelineElementInt -> {
                            ConfidentialTimelineElementDtoInt dtoInt = mapConf.get(timelineElementInt.getElementId());
                            if (dtoInt != null) {
                                enrichTimelineElementWithConfidentialInformation(timelineElementInt.getDetails(), dtoInt);
                            }
                        }
                );
            }
        }

        return setTimelineElements;
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("getTimelineAndStatusHistory Start - iun={} ", iun);
        
        Set<TimelineElementInternal> timelineElements = getTimeline(iun, true);
        
        List<NotificationStatusHistoryElementInt> statusHistory = statusUtils
                .getStatusHistory(timelineElements, numberOfRecipients, createdAt);

        removeNotToBeReturnedElements(statusHistory);

        NotificationStatusInt currentStatus = statusUtils.getCurrentStatus(statusHistory);

        log.debug("getTimelineAndStatusHistory Ok - iun={} ", iun);

        return createResponse(timelineElements, statusHistory, currentStatus);
    }

    private void removeNotToBeReturnedElements(List<NotificationStatusHistoryElementInt> statusHistory) {

        //Viene eliminato l'elemento InValidation dalla response
        Optional<Instant> inValidationStatusActiveFromOpt = Optional.empty();

        for (NotificationStatusHistoryElementInt element : statusHistory) {

            if (NotificationStatusInt.IN_VALIDATION.equals(element.getStatus())) {
                inValidationStatusActiveFromOpt = Optional.of(element.getActiveFrom());
                statusHistory.remove(element);
                break;
            }
        }

        if (inValidationStatusActiveFromOpt.isPresent()) {

            //Viene sostituito il campo ActiveFrom dell'elemento ACCEPTED con quella dell'elemento eliminato IN_VALIDATION
            Instant inValidationStatusActiveFrom = inValidationStatusActiveFromOpt.get();

            statusHistory.stream()
                    .filter(
                            statusHistoryElement -> NotificationStatusInt.ACCEPTED.equals(statusHistoryElement.getStatus())
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

        if (details instanceof CourtesyAddressRelatedTimelineElement && confidentialDto.getDigitalAddress() != null) {
            CourtesyDigitalAddressInt address = ((CourtesyAddressRelatedTimelineElement) details).getDigitalAddress();

            address = getCourtesyDigitalAddress(confidentialDto, address);
            ((CourtesyAddressRelatedTimelineElement) details).setDigitalAddress(address);
        }

        if (details instanceof DigitalAddressRelatedTimelineElement && confidentialDto.getDigitalAddress() != null) {

            LegalDigitalAddressInt address = ((DigitalAddressRelatedTimelineElement) details).getDigitalAddress();

            address = getDigitalAddress(confidentialDto, address);

            ((DigitalAddressRelatedTimelineElement) details).setDigitalAddress(address);
        }

        if (details instanceof PhysicalAddressRelatedTimelineElement && confidentialDto.getPhysicalAddress() != null) {
            PhysicalAddressInt physicalAddress = ((PhysicalAddressRelatedTimelineElement) details).getPhysicalAddress();

            physicalAddress = getPhysicalAddress(physicalAddress, confidentialDto.getPhysicalAddress());

            ((PhysicalAddressRelatedTimelineElement) details).setPhysicalAddress(physicalAddress);
        }

        if (details instanceof NewAddressRelatedTimelineElement && confidentialDto.getNewPhysicalAddress() != null) {

            PhysicalAddressInt newAddress = ((NewAddressRelatedTimelineElement) details).getNewAddress();

            newAddress = getPhysicalAddress(newAddress, confidentialDto.getNewPhysicalAddress());

            ((NewAddressRelatedTimelineElement) details).setNewAddress(newAddress);

        }
    }

    private LegalDigitalAddressInt getDigitalAddress(ConfidentialTimelineElementDtoInt confidentialDto, LegalDigitalAddressInt address) {
        if (address == null) {
            address = LegalDigitalAddressInt.builder().build();
        }

        address = address.toBuilder().address(confidentialDto.getDigitalAddress()).build();
        return address;
    }

    private CourtesyDigitalAddressInt getCourtesyDigitalAddress(ConfidentialTimelineElementDtoInt confidentialDto, CourtesyDigitalAddressInt address) {
        if (address == null) {
            address = CourtesyDigitalAddressInt.builder().build();
        }

        address = address.toBuilder().address(confidentialDto.getDigitalAddress()).build();
        return address;
    }

    private PhysicalAddressInt getPhysicalAddress(PhysicalAddressInt physicalAddress, PhysicalAddressInt physicalAddress2) {
        if (physicalAddress == null) {
            physicalAddress = PhysicalAddressInt.builder().build();
        }

        return physicalAddress.toBuilder()
                .at(physicalAddress2.getAt())
                .address(physicalAddress2.getAddress())
                .municipality(physicalAddress2.getMunicipality())
                .province(physicalAddress2.getProvince())
                .addressDetails(physicalAddress2.getAddressDetails())
                .zip(physicalAddress2.getZip())
                .municipalityDetails(physicalAddress2.getMunicipalityDetails())
                .build();
    }

    protected StatusInfoEntity buildStatusInfo(StatusService.NotificationStatusUpdate notificationStatuses) {
        Instant statusChangeTimestamp = null;
        boolean statusChanged = false;

        if (isStatusChanged(notificationStatuses)) {
            statusChanged = true;
            statusChangeTimestamp = Instant.now();
        }

        return StatusInfoEntity.builder()
                .statusChanged(statusChanged)
                .statusChangeTimestamp(statusChangeTimestamp)
                .actual(notificationStatuses.getNewStatus().getValue())
                .build();
    }

    private boolean isStatusChanged(StatusService.NotificationStatusUpdate notificationStatuses) {
        return notificationStatuses.getOldStatus() != notificationStatuses.getNewStatus();
    }


}
