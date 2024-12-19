package it.pagopa.pn.deliverypush.service.impl;

import static it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;
import static it.pagopa.pn.deliverypush.utils.StatusUtils.*;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
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
import it.pagopa.pn.deliverypush.dto.timeline.details.CourtesyAddressRelatedTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalAddressRelatedTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.NewAddressRelatedTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.PersonalInformationRelatedTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.PhysicalAddressRelatedTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationRecipientIdNotValidException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableSchedulingAnalogDateResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV23;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineCounterEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.StatusService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationStatusHistoryElementMapper;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import it.pagopa.pn.deliverypush.service.mapper.TimelineElementMapper;
import it.pagopa.pn.deliverypush.utils.MdcKey;
import it.pagopa.pn.deliverypush.utils.StatusUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;
    private final TimelineCounterEntityDao timelineCounterEntityDao;
    private final StatusUtils statusUtils;
    private final ConfidentialInformationService confidentialInformationService;
    private final StatusService statusService;

    private final NotificationService notificationService;
    @Qualifier("lockProviderTimeline")
    private final LockProvider lockProvider;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    //TODO: da modificare per includere la category del deceduto
    public static final Set<TimelineElementCategoryInt> COMPLETED_DELIVERY_WORKFLOW_CATEGORY = new HashSet<>() {{
        addAll(SUCCES_DELIVERY_WORKFLOW_CATEGORY);
        addAll(FAILURE_DELIVERY_WORKFLOW_CATEGORY);
    }};


    @Override
    public boolean addTimelineElement(TimelineElementInternal dto, NotificationInt notification) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.TIMELINE_KEY);

        log.debug("addTimelineElement - IUN={} and timelineId={}", dto.getIun(), dto.getElementId());
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = getPnAuditLogEvent(dto, auditLogBuilder);
        logEvent.log();


        if (notification != null) {
            boolean isMultiRecipient = notification.getRecipients().size() > 1;
            boolean isCriticalTimelineElement = COMPLETED_DELIVERY_WORKFLOW_CATEGORY.contains(dto.getCategory());
            if (isMultiRecipient && isCriticalTimelineElement) {
                return addCriticalTimelineElement(dto, notification, logEvent);
            }

            return addTimelineElement(dto, notification, logEvent);

        } else {
            MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
            logEvent.generateFailure("Try to update Timeline and Status for non existing iun={}", dto.getIun());
            throw new PnInternalException("Try to update Timeline and Status for non existing iun " + dto.getIun(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED);
        }

    }

    private boolean addCriticalTimelineElement(TimelineElementInternal dto, NotificationInt notification, PnAuditLogEvent logEvent) {
        log.debug("addCriticalTimelineElement - IUN={} and timelineId={}", dto.getIun(), dto.getElementId());

        Optional<SimpleLock> optSimpleLock = lockProvider.lock(new LockConfiguration(Instant.now(), notification.getIun(), pnDeliveryPushConfigs.getTimelineLockDuration(), Duration.ofNanos(1)));
        if (optSimpleLock.isEmpty()) {
            logEvent.generateFailure("Lock not acquired for iun={} and timeline with category={}", notification.getIun(), dto.getCategory()).log();
            throw new PnInternalException("Lock not acquired for iun " + notification.getIun(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED);
        }

        SimpleLock simpleLock = optSimpleLock.get();

        try {
            return processTimelinePersistence(dto, notification, logEvent);
        } catch (Exception ex) {
            MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
            logEvent.generateFailure("Exception in addTimelineElement", ex).log();
            throw new PnInternalException("Exception in addTimelineElement - iun=" + notification.getIun() + " elementId=" + dto.getElementId(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED, ex);
        } finally {
            simpleLock.unlock();
        }
    }

    private boolean addTimelineElement(TimelineElementInternal dto, NotificationInt notification, PnAuditLogEvent logEvent) {
        try {
            return processTimelinePersistence(dto, notification, logEvent);
        } catch (Exception ex) {
            MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
            logEvent.generateFailure("Exception in addTimelineElement", ex).log();
            throw new PnInternalException("Exception in addTimelineElement - iun=" + notification.getIun() + " elementId=" + dto.getElementId(), ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED, ex);
        }
    }

    private boolean processTimelinePersistence(TimelineElementInternal dto, NotificationInt notification, PnAuditLogEvent logEvent) {
        boolean timelineInsertSkipped;
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

        // non schedulo più il webhook in questo punto (schedulerService.scheduleWebhookEvent), dato che la cosa viene fatta in maniera
        // asincrona da una lambda che opera partendo da stream Kinesis

        String alreadyInsertMsg = "Timeline event was already inserted before - timelineId=" + dto.getElementId();
        String successMsg = String.format("Timeline event inserted with: CATEGORY=%s IUN=%s {DETAILS: %s} TIMELINEID=%s paId=%s TIMESTAMP=%s",
                dto.getCategory(),
                dto.getIun(),
                dto.getDetails() != null ? dto.getDetails().toLog() : null,
                dto.getElementId(),
                dto.getPaId(),
                dto.getTimestamp());
        logEvent.generateSuccess(timelineInsertSkipped ? alreadyInsertMsg : successMsg).log();

        MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);

        return timelineInsertSkipped;
    }

    private boolean persistTimelineElement(TimelineElementInternal dtoWithStatusInfo) {
        try {
            timelineDao.addTimelineElementIfAbsent(dtoWithStatusInfo);
        } catch (PnIdConflictException ex) {
            log.warn("Exception idconflict is expected for retry, letting flow continue");
            return true;
        }
        return false;
    }

    private PnAuditLogEvent getPnAuditLogEvent(TimelineElementInternal dto, PnAuditLogBuilder auditLogBuilder) {
        String auditLog = String.format("Timeline event inserted with: CATEGORY=%s IUN=%s {DETAILS: %s} TIMELINEID=%s paId=%s TIMESTAMP=%s",
                dto.getCategory(),
                dto.getIun(),
                dto.getDetails() != null ? dto.getDetails().toLog() : null,
                dto.getElementId(),
                dto.getPaId(),
                dto.getTimestamp());
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_TIMELINE, auditLog)
                .iun(dto.getIun())
                .build();
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN={} and timelineId={}", iun, timelineId);

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineDao.getTimelineElement(iun, timelineId);
        return addConfidentialInformationIfTimelineElementIsPresent(iun, timelineId, timelineElementInternalOpt);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId) {
        log.debug("GetTimelineElement - IUN= {} and timelineId= {}", iun, timelineId);

        Optional<TimelineElementInternal> timelineElementInternalOpt = timelineDao.getTimelineElementStrongly(iun, timelineId);
        return addConfidentialInformationIfTimelineElementIsPresent(iun, timelineId, timelineElementInternalOpt);
    }

    private Optional<TimelineElementInternal> addConfidentialInformationIfTimelineElementIsPresent(String iun, String timelineId, Optional<TimelineElementInternal> timelineElementInternalOpt) {
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

                    if (x.getDetails() instanceof RecipientRelatedTimelineElementDetails recRelatedTimelineElementDetails) {
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

                    if (timelineDetailsClass.isInstance(x.getDetails()) && x.getDetails() instanceof RecipientRelatedTimelineElementDetails recRelatedTimelineElementDetails) {
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
        setConfidentialInfo(confidentialInfoRequired, iun, setTimelineElements);
        return setTimelineElements;
    }

    private void setConfidentialInfo(boolean confidentialInfoRequired, String iun, Set<TimelineElementInternal> setTimelineElements) {
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
    }

    @Override
    public Set<TimelineElementInternal> getTimelineStrongly(String iun, boolean confidentialInfoRequired) {
        log.debug("GetTimelineStrongly - iun={} ", iun);
        Set<TimelineElementInternal> setTimelineElements = this.timelineDao.getTimelineStrongly(iun);
        setConfidentialInfo(confidentialInfoRequired, iun, setTimelineElements);
        return setTimelineElements;
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        log.debug("getTimelineByIunTimelineId - iun={} timelineId={}", iun, timelineId);
        Set<TimelineElementInternal> setTimelineElements = this.timelineDao.getTimelineFilteredByElementId(iun, timelineId);
        setConfidentialInfo(confidentialInfoRequired, iun, setTimelineElements);
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
                .map(t -> SmartMapper.mapTimelineInternal(t, timelineElements)) // rimappo su se stessa, per sistemare eventuali campi interni
                .sorted(Comparator.naturalOrder())
                .filter(this::isNotDiagnosticTimelineElement)
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

    public boolean isNotDiagnosticTimelineElement(TimelineElementInternal timelineElementInternal) {
        if (timelineElementInternal.getCategory() == null) {
            return true;
        }
        String internalCategory = timelineElementInternal.getCategory().getValue();
        return Arrays.stream(TimelineElementCategoryV23.values())
                .anyMatch(timelineElementCategoryV23 -> timelineElementCategoryV23.getValue().equalsIgnoreCase(internalCategory));

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
        for (int i = 0; i < notificationInt.getRecipients().size(); i++) {
            if (notificationInt.getRecipients().get(i).getInternalId().equals(recipientId)) {
                return i;
            }
        }

        throw new PnValidationRecipientIdNotValidException(String.format("Recipient %s not found", recipientId));
    }

    @Override
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
        } else {
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
