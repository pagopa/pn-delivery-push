package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdIntWithRecIndex;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnValidationRecipientIdNotValidException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.LegalFactIdMapper;
import it.pagopa.pn.deliverypush.service.mapper.TimelineServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimelineServiceHttpImpl implements TimelineService {

    private final TimelineClient timelineClient;
    private final NotificationService notificationService;
    private final TimelineServiceMapper timelineServiceMapper;

    @Override
    public boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        log.info("addTimelineElement - IUN={} and timelineId={}", element.getIun(), element.getElementId());
        return timelineClient.addTimelineElement(element, notification);
    }

    @Override
    public Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId) {
        log.debug("getTimelineElement - IUN={} and timelineId={}", iun, timelineId);
        return Optional.ofNullable(timelineClient.getTimelineElement(iun, timelineId, false));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetails - IUN={} and timelineId={}", iun, timelineId);

        TimelineElementDetailsInt timelineElementDetailsInt = timelineClient.getTimelineElementDetails(iun, timelineId);

        return castInternalDetails(timelineDetailsClass, timelineElementDetailsInt);
    }

    private <T> @NotNull Optional<T> castInternalDetails(Class<T> timelineDetailsClass, TimelineElementDetailsInt timelineElementDetailsInt) {
        if( timelineElementDetailsInt == null) {
            return Optional.empty();
        }
        return Optional.of(timelineDetailsClass.cast(timelineElementDetailsInt));
    }

    @Override
    public <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass) {
        log.debug("getTimelineElementDetailForSpecificRecipient - IUN={}, recIndex={}, confidentialInfoRequired={}, category={}", iun, recIndex, confidentialInfoRequired, category);

        TimelineElementDetailsInt timelineElementDetailsInt = timelineClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);
        return castInternalDetails(timelineDetailsClass, timelineElementDetailsInt);
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired) {
        log.debug("getTimeline - IUN={} and confidentialInfoRequired={}", iun, confidentialInfoRequired);

        return new HashSet<>(Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, null))
                .orElseGet(Collections::emptyList));
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        log.debug("getTimelineByIunTimelineId - IUN={}, timelineId={}, confidentialInfoRequired={}", iun, timelineId, confidentialInfoRequired);

        return new HashSet<>(Optional.ofNullable(timelineClient.getTimeline(iun, confidentialInfoRequired, false, timelineId))
                .orElseGet(Collections::emptyList));
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("getTimelineAndStatusHistory - IUN={}, numberOfRecipients={}, createdAt={}", iun, numberOfRecipients, createdAt);

        it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse notificationHistoryResponse =
                timelineClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
        removeDiagnosticElements(notificationHistoryResponse);
        return timelineServiceMapper.toNotificationHistoryResponseDto(notificationHistoryResponse);
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
        return Arrays.stream(TimelineElementCategoryV28.values())
                .anyMatch(enumVal -> enumVal.getValue().equalsIgnoreCase(elementCategory));
    }

    private int getRecipientIndex(NotificationInt notificationInt, String recipientId) {
        for (int i = 0; i < notificationInt.getRecipients().size(); i++) {
            if (notificationInt.getRecipients().get(i).getInternalId().equals(recipientId)) {
                return i;
            }
        }

        throw new PnValidationRecipientIdNotValidException(String.format("Recipient %s not found", recipientId));
    }

    public List<LegalFactsIdIntWithRecIndex> getLegalFacts(String iun, Integer recIndex) {
        log.debug("getLegalFacts - IUN={}, recIndex={}", iun, recIndex);

        LegalFactsResponse legalFactsResponse = timelineClient.getLegalFacts(iun, recIndex);
        return LegalFactIdMapper.toLegalFactsIdIntWithRecIndex(legalFactsResponse);
    }
}
