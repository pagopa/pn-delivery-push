package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationRecipientIdNotValidException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableSchedulingAnalogDateResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.TimelineServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimelineServiceHttpImpl implements TimelineService {

    private final TimelineClient timelineClient;
    private final NotificationService notificationService;

    @Override
    public boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        log.debug("addTimelineElement - IUN={} and timelineId={}", element.getIun(), element.getElementId());

        NewTimelineElement newTimelineElement = TimelineServiceMapper.getNewTimelineElement(element, notification);
        return timelineClient.addTimelineElement(newTimelineElement);
    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        log.debug("retrieveAndIncrementCounterForTimelineEvent - timelineId={}", timelineId);

        return timelineClient.retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("getTimelineElement - IUN={} and timelineId={}", iun, timelineId);

        TimelineElement timelineElement = timelineClient.getTimelineElement(iun, timelineId, false);
        return Optional.ofNullable(TimelineServiceMapper.toTimelineElementInternal(timelineElement));
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId) {
        log.debug("getTimelineElementStrongly - IUN={} and timelineId={}", iun, timelineId);

        TimelineElement timelineElement = timelineClient.getTimelineElement(iun, timelineId, true);
        return Optional.ofNullable(TimelineServiceMapper.toTimelineElementInternal(timelineElement));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetails - IUN={} and timelineId={}", iun, timelineId);

        TimelineElementDetails timelineElementDetails = timelineClient.getTimelineElementDetails(iun, timelineId);

        return getTimelineElementDetailsInt(timelineDetailsClass, timelineElementDetails);
    }

    private static <T> @NotNull Optional<T> getTimelineElementDetailsInt(Class<T> timelineDetailsClass, TimelineElementDetails timelineElementDetails) {
        if( timelineElementDetails == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(timelineDetailsClass.cast(TimelineServiceMapper.toTimelineElementDetailsInt(
                timelineElementDetails, TimelineElementCategoryInt.valueOf(timelineElementDetails.getCategoryType()))));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetailForSpecificRecipient - IUN={}, recIndex={}, confidentialInfoRequired={}, category={}", iun, recIndex, confidentialInfoRequired, category);

        TimelineElementDetails timelineElementDetails = timelineClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, TimelineCategory.fromValue(category.getValue()));
        return getTimelineElementDetailsInt(timelineDetailsClass,timelineElementDetails);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElementForSpecificRecipient(String iun, int recIndex, TimelineElementCategoryInt category) {
        log.debug("getTimelineElementForSpecificRecipient - IUN={}, recIndex={}, category={}", iun, recIndex, category);

        TimelineElement timelineElement = timelineClient.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineCategory.fromValue(category.getValue()));
        return Optional.ofNullable(TimelineServiceMapper.toTimelineElementInternal(timelineElement));
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired) {
        log.debug("getTimeline - IUN={} and confidentialInfoRequired={}", iun, confidentialInfoRequired);

        return Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, null))
                .orElseGet(Collections::emptyList)
                .stream()
                .map(TimelineServiceMapper::toTimelineElementInternal)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TimelineElementInternal> getTimelineStrongly(String iun, boolean confidentialInfoRequired) {
        log.debug("getTimelineStrongly - IUN={} and confidentialInfoRequired={}", iun, confidentialInfoRequired);

        return Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, true, null))
                .orElseGet(Collections::emptyList)
                .stream()
                .map(TimelineServiceMapper::toTimelineElementInternal)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        log.debug("getTimelineByIunTimelineId - IUN={}, timelineId={}, confidentialInfoRequired={}", iun, timelineId, confidentialInfoRequired);

        return Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, timelineId))
                .orElseGet(Collections::emptyList)
                .stream()
                .map(TimelineServiceMapper::toTimelineElementInternal)
                .collect(Collectors.toSet());
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("getTimelineAndStatusHistory - IUN={}, numberOfRecipients={}, createdAt={}", iun, numberOfRecipients, createdAt);

        it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse notificationHistoryResponse =
                timelineClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
        removeDiagnosticElements(notificationHistoryResponse);
        return TimelineServiceMapper.toNotificationHistoryResponseDto(notificationHistoryResponse);
    }

    private void removeDiagnosticElements(it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse notificationHistoryResponse) {
        if (notificationHistoryResponse.getTimeline() != null) {
            notificationHistoryResponse.setTimeline(
                    notificationHistoryResponse.getTimeline().stream()
                            .filter(timelineElement -> isPublicElement(timelineElement.getCategory().getValue()))
                            .collect(Collectors.toList())
            );
            if (notificationHistoryResponse.getNotificationStatusHistory() != null) {
                // Ottieni gli id degli elementi rimasti nella timeline
                Set<String> timelineIds = getTimelineIds(notificationHistoryResponse);
                notificationHistoryResponse.setNotificationStatusHistory(
                        notificationHistoryResponse.getNotificationStatusHistory().stream()
                            .map(statusHistoryElement -> {
                                filterRelatedTimelineElements(statusHistoryElement, timelineIds);
                                return statusHistoryElement;
                            }).toList()
                );
            }
        }
    }

    private Set<String> getTimelineIds(it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse notificationHistoryResponse) {
        return Objects.requireNonNull(notificationHistoryResponse.getTimeline())
                .stream()
                .map(TimelineElement::getElementId)
                .collect(Collectors.toSet());
    }

    private void filterRelatedTimelineElements(NotificationStatusHistoryElement statusHistoryElement, Set<String> timelineIds) {
        List<String> filteredRelated = statusHistoryElement.getRelatedTimelineElements().stream()
                .filter(timelineIds::contains)
                .toList();
        statusHistoryElement.setRelatedTimelineElements(filteredRelated);
    }

    private boolean isPublicElement(String elementCategory) {
        return Arrays.stream(TimelineElementCategoryV27.values())
                .anyMatch(enumVal -> enumVal.getValue().equalsIgnoreCase(elementCategory));
    }

    @Override
    public Mono<ProbableSchedulingAnalogDateResponse> getSchedulingAnalogDate(String iun, String recipientId) {
        log.debug("getSchedulingAnalogDate - IUN={}, recipientId={}", iun, recipientId);


        return notificationService.getNotificationByIunReactive(iun)
                .map(notificationRecipientInts -> getRecipientIndex(notificationRecipientInts, recipientId))
                .map(recIndex -> this.getTimelineElementDetailForSpecificRecipient(iun,recIndex,false, TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE, ProbableDateAnalogWorkflowDetailsInt.class))
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
}
