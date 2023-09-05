package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationRecipientIdNotValidException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineCounterEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.service.mapper.NotificationStatusHistoryElementMapper;
import it.pagopa.pn.deliverypush.service.mapper.TimelineElementMapper;
import it.pagopa.pn.deliverypush.utils.MdcKey;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;
    private final TimelineCounterEntityDao timelineCounterEntityDao;
    private final StatusUtils statusUtils;
    private final ConfidentialInformationService confidentialInformationService;
    private final StatusService statusService;
    private final SchedulerService schedulerService;
    private final NotificationService notificationService;


    @Override
    public boolean addTimelineElement(TimelineElementInternal dto, NotificationInt notification) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.TIMELINE_KEY);
        
        log.debug("addTimelineElement - IUN={} and timelineId={}", dto.getIun(), dto.getElementId());
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = getPnAuditLogEvent(dto, auditLogBuilder);
        logEvent.log();
        boolean timelineInsertSkipped;

        if (notification != null) {
            try{
                Set<TimelineElementInternal> currentTimeline = getTimeline(dto.getIun(), true);
                StatusService.NotificationStatusUpdate notificationStatuses = statusService.getStatus(dto, currentTimeline, notification);

                // vengono salvate le informazioni confidenziali in sicuro, dal momento che successivamente non saranno salvate a DB
                confidentialInformationService.saveTimelineConfidentialInformation(dto);

                // aggiungo al DTO lo status info che poi verrà mappato sull'entity e salvato
                TimelineElementInternal dtoWithStatusInfo = enrichWithStatusInfo(dto, currentTimeline, notificationStatuses, notification.getSentAt());

                timelineInsertSkipped = persistTimelineElement(dtoWithStatusInfo);


                // aggiorna lo stato su pn-delivery se i due stati differiscono
                if (!notificationStatuses.getOldStatus().equals(notificationStatuses.getNewStatus())) {
                    statusService.updateStatus(dto.getIun(), notificationStatuses.getNewStatus(), dto.getTimestamp());
                }

                // genero un messaggio per l'aggiunta in sqs in modo da salvarlo in maniera asincrona
                schedulerService.scheduleWebhookEvent(
                        notification.getSender().getPaId(),
                        dtoWithStatusInfo.getIun(),
                        dtoWithStatusInfo.getElementId()
                );

                String successMsg = "Timeline event inserted with iun=" + dto.getIun() + " elementId = " + dto.getElementId();
                logEvent.generateSuccess(timelineInsertSkipped?"Timeline event was already inserted before": successMsg).log();
                
                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
                
                return timelineInsertSkipped;
            } catch (Exception ex) {
                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
                logEvent.generateFailure("Exception in addTimelineElement", ex).log();
                throw new PnInternalException("Exception in addTimelineElement - iun=" + notification.getIun() + " elementId=" + dto.getElementId(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED, ex);
            }

        } else {
            MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
            logEvent.generateFailure("Try to update Timeline and Status for non existing iun={}", dto.getIun());
            throw new PnInternalException("Try to update Timeline and Status for non existing iun " + dto.getIun(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED);
        }

    }

    private boolean persistTimelineElement(TimelineElementInternal dtoWithStatusInfo) {
        try {
            timelineDao.addTimelineElementIfAbsent(dtoWithStatusInfo);
        } catch (PnIdConflictException ex){
            log.warn("Exception idconflict is expected for retry, letting flow continue");
            return true;
        }
        return false;
    }

    private PnAuditLogEvent getPnAuditLogEvent(TimelineElementInternal dto, PnAuditLogBuilder auditLogBuilder) {
        String auditLog = String.format(
                "Add timeline element: CATEGORY=%s IUN=%s {DETAILS: %s} TIMELINEID=%s TIMESTAMP=%s",
                dto.getCategory(),
                dto.getIun(),
                dto.getDetails() != null ? dto.getDetails().toLog() : null,
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

    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        return this.timelineCounterEntityDao.getCounter(timelineId).getCounter();
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
    public Optional<TimelineElementInternal> getTimelineElementForSpecificRecipient(String iun, int recIndex, TimelineElementCategoryInt category) {
        log.debug("getTimelineElementForSpecificRecipient - IUN={} and recIndex={}", iun, recIndex);

        return this.timelineDao.getTimeline(iun)
                .stream().filter(x -> x.getCategory().equals(category))
                .filter(x -> {

                    if ( x.getDetails() instanceof RecipientRelatedTimelineElementDetails recRelatedTimelineElementDetails){
                        return recRelatedTimelineElementDetails.getRecIndex() == recIndex;
                    }
                    return false;
                })
                .findFirst();
    }

    @Override
    public <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetailForSpecificIndex - IUN={} and recIndex={}", iun, recIndex);
        
        Optional<TimelineElementInternal> timelineElementOpt = this.timelineDao.getTimeline(iun)
                .stream().filter(x -> x.getCategory().equals(category))
                .filter(x -> {
                    
                    if ( timelineDetailsClass.isInstance(x.getDetails()) && x.getDetails() instanceof RecipientRelatedTimelineElementDetails recRelatedTimelineElementDetails){
                        return recRelatedTimelineElementDetails.getRecIndex() == recIndex;
                    }
                    return false;
                })
                .findFirst();
        
        if (timelineElementOpt.isPresent()) {
            TimelineElementInternal timelineElement = timelineElementOpt.get();

            if (confidentialInfoRequired) {
                confidentialInformationService.getTimelineElementConfidentialInformation(iun, timelineElement.getElementId()).ifPresent(
                        confidentialDto -> enrichTimelineElementWithConfidentialInformation(
                                timelineElement.getDetails(), confidentialDto
                        )
                );
            }

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
        log.debug("getTimelineByIunTimelineId - iun={} timelineId={}", iun, timelineId);
        Set<TimelineElementInternal> setTimelineElements =  this.timelineDao.getTimelineFilteredByElementId(iun, timelineId);

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

        var timelineList = timelineElements.stream()
                .sorted(Comparator.naturalOrder())
                .map(TimelineElementMapper::internalToExternal)
                .toList();

        return NotificationHistoryResponse.builder()
                .timeline(timelineList)
                .notificationStatusHistory(
                        statusHistory.stream().map(
                                NotificationStatusHistoryElementMapper::internalToExternal
                        ).toList()
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

    @Override
    public Mono<ProbableSchedulingAnalogDateResponse> getSchedulingAnalogDate(String iun, String recipientId) {

        return notificationService.getNotificationByIunReactive(iun)
                .map(notificationRecipientInts -> getRecipientIndex(notificationRecipientInts, recipientId))
                .map(recIndex -> getTimelineElementDetailForSpecificRecipient(iun, recIndex, false, PROBABLE_SCHEDULING_ANALOG_DATE, ProbableDateAnalogWorkflowDetailsInt.class))
                .flatMap(optionalDetails -> optionalDetails.map(Mono::just).orElseGet(Mono::empty))
                .map(details -> new ProbableSchedulingAnalogDateResponse()
                        .iun(iun)
                        .recIndex(details.getRecIndex())
                        .schedulingAnalogDate(details.getSchedulingAnalogDate()))
                .switchIfEmpty(Mono.error(() -> {
                    String message = String.format("ProbableSchedulingDateAnalog not found for iun: %s, recipientId: %s", iun, recipientId);
                    return new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
                }));

    }

    private int getRecipientIndex(NotificationInt notificationInt, String recipientId) {
        for(int i = 0; i < notificationInt.getRecipients().size(); i++ ) {
            if(notificationInt.getRecipients().get(i).getInternalId().equals(recipientId)) {
                return i;
            }
        }

        throw new PnValidationRecipientIdNotValidException(String.format("Recipient %s not found", recipientId));
    }

    public void enrichTimelineElementWithConfidentialInformation(TimelineElementDetailsInt details,
                                                                 ConfidentialTimelineElementDtoInt confidentialDto) {

        if (details instanceof CourtesyAddressRelatedTimelineElement courtesyAddressRelatedTimelineElement && confidentialDto.getDigitalAddress() != null) {
            CourtesyDigitalAddressInt address = courtesyAddressRelatedTimelineElement.getDigitalAddress();

            address = getCourtesyDigitalAddress(confidentialDto, address);
            ((CourtesyAddressRelatedTimelineElement) details).setDigitalAddress(address);
        }

        if (details instanceof DigitalAddressRelatedTimelineElement digitalAddressRelatedTimelineElement && confidentialDto.getDigitalAddress() != null) {

            LegalDigitalAddressInt address = digitalAddressRelatedTimelineElement.getDigitalAddress();

            address = getDigitalAddress(confidentialDto, address);

            ((DigitalAddressRelatedTimelineElement) details).setDigitalAddress(address);
        }

        if (details instanceof PhysicalAddressRelatedTimelineElement physicalAddressRelatedTimelineElement && confidentialDto.getPhysicalAddress() != null) {
            PhysicalAddressInt physicalAddress = physicalAddressRelatedTimelineElement.getPhysicalAddress();

            physicalAddress = getPhysicalAddress(physicalAddress, confidentialDto.getPhysicalAddress());

            ((PhysicalAddressRelatedTimelineElement) details).setPhysicalAddress(physicalAddress);
        }

        if (details instanceof NewAddressRelatedTimelineElement newAddressRelatedTimelineElement && confidentialDto.getNewPhysicalAddress() != null) {

            PhysicalAddressInt newAddress = newAddressRelatedTimelineElement.getNewAddress();

            newAddress = getPhysicalAddress(newAddress, confidentialDto.getNewPhysicalAddress());

            ((NewAddressRelatedTimelineElement) details).setNewAddress(newAddress);
        }

        if (details instanceof PersonalInformationRelatedTimelineElement personalInformationRelatedTimelineElement) {
            personalInformationRelatedTimelineElement.setTaxId(confidentialDto.getTaxId());
            personalInformationRelatedTimelineElement.setDenomination(confidentialDto.getDenomination());
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
                .foreignState(physicalAddress2.getForeignState())
                .build();
    }

    private TimelineElementInternal enrichWithStatusInfo(TimelineElementInternal dto, Set<TimelineElementInternal> currentTimeline,
                                      StatusService.NotificationStatusUpdate notificationStatuses, Instant notificationSentAt) {

        Instant timestampLastTimelineElement = getTimestampLastUpdateStatus(currentTimeline, notificationSentAt);
        StatusInfoInternal statusInfo = buildStatusInfo(notificationStatuses, timestampLastTimelineElement);
        return dto.toBuilder().statusInfo(statusInfo).build();
    }

    private Instant getTimestampLastUpdateStatus(Set<TimelineElementInternal> currentTimeline, Instant notificationSentAt) {
        Optional<StatusInfoInternal> max = currentTimeline.stream()
                .map(TimelineElementInternal::getStatusInfo)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(StatusInfoInternal::getStatusChangeTimestamp));

        return max.map(StatusInfoInternal::getStatusChangeTimestamp).orElse(notificationSentAt);
        
    }

    protected StatusInfoInternal buildStatusInfo(StatusService.NotificationStatusUpdate notificationStatuses,
                                                 Instant timestampLastUpdateStatus) {
        Instant statusChangeTimestamp;
        boolean statusChanged = false;

        if (isStatusChanged(notificationStatuses)) {
            statusChanged = true;
            statusChangeTimestamp = Instant.now();
        }
        else {
            statusChangeTimestamp = timestampLastUpdateStatus;
        }

        return StatusInfoInternal.builder()
                .statusChanged(statusChanged)
                .statusChangeTimestamp(statusChangeTimestamp)
                .actual(notificationStatuses.getNewStatus().getValue())
                .build();
    }

    private boolean isStatusChanged(StatusService.NotificationStatusUpdate notificationStatuses) {
        return notificationStatuses.getOldStatus() != notificationStatuses.getNewStatus();
    }


}
