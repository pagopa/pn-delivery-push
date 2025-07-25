package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;

import java.util.Collections;
import java.util.List;

public class TimelineServiceMapper {

    private TimelineServiceMapper() {
    }

    public static NewTimelineElement getNewTimelineElement(TimelineElementInternal timelineElementInternal,
                                               NotificationInt notificationInt) {
        return new NewTimelineElement()
                .timelineElement(toTimelineElement(timelineElementInternal))
                .notificationInfo(toNotificationInfo(notificationInt));
    }

    public static TimelineElementInternal toTimelineElementInternal(TimelineElement timelineElement) {
        if (timelineElement == null) {
            return null;
        }
        TimelineElementCategoryInt category = TimelineElementCategoryInt.valueOf(timelineElement.getCategory().getValue());

        return TimelineElementInternal.builder()
                .iun(timelineElement.getIun())
                .elementId(timelineElement.getElementId())
                .timestamp(timelineElement.getTimestamp())
                .paId(timelineElement.getPaId())
                .legalFactsIds(timelineElement.getLegalFactsIds() != null ? toLegalFactsIdIntList(timelineElement.getLegalFactsIds()) : null)
                .category(category)
                .details(toTimelineElementDetailsInt(timelineElement.getDetails(), category))
                .statusInfo(toStatusInfoInternal(timelineElement.getStatusInfo()))
                .notificationSentAt(timelineElement.getNotificationSentAt())
                .ingestionTimestamp(timelineElement.getIngestionTimestamp())
                .eventTimestamp(timelineElement.getEventTimestamp())
                .build();
    }

    public static it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse toNotificationHistoryResponseDto(
            it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse source) {

        if (source == null) {
            return null;
        }

        return it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse.builder()
                .notificationStatus(getNotificationStatus(source))
                .notificationStatusHistory(getNotificationStatusHistoryElementList(source))
                .timeline(getTimelineElementV28List(source))
                .build();
    }

    private static List<TimelineElementV28> getTimelineElementV28List(NotificationHistoryResponse source) {
        List<TimelineElementV28> timeline = null;
        if (source.getTimeline() != null) {
            timeline = source.getTimeline().stream()
                    .map(item -> TimelineElementV28.builder()
                            .elementId(item.getElementId())
                            .timestamp(item.getTimestamp())
                            .legalFactsIds(item.getLegalFactsIds() != null ? toLegalFactsIdV28List(item.getLegalFactsIds()) : null)
                            .category(TimelineElementCategoryV28.valueOf(item.getCategory().getValue()))
                            .details(toTimelineElementDetailsV28(item.getDetails()))
                            .notificationSentAt(item.getNotificationSentAt())
                            .ingestionTimestamp(item.getIngestionTimestamp())
                            .eventTimestamp(item.getEventTimestamp())
                            .build())
                    .toList();
        }
        return timeline;
    }

    private static List<NotificationStatusHistoryElementV28> getNotificationStatusHistoryElementList(NotificationHistoryResponse source) {
        List<NotificationStatusHistoryElementV28> notificationStatusHistory = null;
        if (source.getNotificationStatusHistory() != null) {
            notificationStatusHistory = source.getNotificationStatusHistory().stream()
                    .map(item -> NotificationStatusHistoryElementV28.builder()
                            .status(NotificationStatusV28.valueOf(item.getStatus().getValue()))
                            .activeFrom(item.getActiveFrom())
                            .relatedTimelineElements(item.getRelatedTimelineElements())
                            .build())
                    .toList();
        }
        return notificationStatusHistory;
    }

    private static NotificationStatusV28 getNotificationStatus(NotificationHistoryResponse source) {
        NotificationStatusV28 notificationStatus = null;
        if (source.getNotificationStatus() != null) {
            notificationStatus = NotificationStatusV28.valueOf(source.getNotificationStatus().getValue());
        }
        return notificationStatus;
    }

    private static NotificationInfo toNotificationInfo(NotificationInt notificationInt) {
        return new NotificationInfo()
                .iun(notificationInt.getIun())
                .paProtocolNumber(notificationInt.getPaProtocolNumber())
                .sentAt(notificationInt.getSentAt())
                .numberOfRecipients(notificationInt.getRecipients() != null ? notificationInt.getRecipients().size() : null);
    }

    private static TimelineElement toTimelineElement(TimelineElementInternal timelineElementInternal) {
        return new TimelineElement()
                .iun(timelineElementInternal.getIun())
                .elementId(timelineElementInternal.getElementId())
                .timestamp(timelineElementInternal.getTimestamp())
                .paId(timelineElementInternal.getPaId())
                .legalFactsIds(timelineElementInternal.getLegalFactsIds() != null ? toLegalFactsIdList(timelineElementInternal.getLegalFactsIds()) : null)
                .category(TimelineCategory.valueOf(timelineElementInternal.getCategory().getValue()))
                .details(toTimelineElementDetails(timelineElementInternal.getDetails(), timelineElementInternal.getCategory().getValue()))
                .notificationSentAt(timelineElementInternal.getNotificationSentAt());
    }

    private static List<LegalFactsId> toLegalFactsIdList(List<LegalFactsIdInt> legalFactsIdIntList) {
        if (legalFactsIdIntList.isEmpty()) {
            return Collections.emptyList();
        }

        return legalFactsIdIntList.stream()
                .map(legalFactsIdInt -> new LegalFactsId()
                        .key(legalFactsIdInt.getKey())
                        .category(LegalFactsId.CategoryEnum.valueOf(legalFactsIdInt.getCategory().getValue())))
                .toList();
    }

    private static List<LegalFactsIdInt> toLegalFactsIdIntList(List<LegalFactsId> legalFactsIdList) {
        if (legalFactsIdList.isEmpty()) {
            return Collections.emptyList();
        }
        return legalFactsIdList.stream()
                .map(legalFactsId -> {
                    assert legalFactsId.getCategory() != null;
                    return LegalFactsIdInt.builder()
                            .key(legalFactsId.getKey())
                            .category(LegalFactCategoryInt.valueOf(legalFactsId.getCategory().getValue()))
                            .build();
                })
                .toList();
    }

    private static List<LegalFactsIdV28> toLegalFactsIdV28List(List<LegalFactsId> legalFactsIdList) {
        if (legalFactsIdList.isEmpty()) {
            return Collections.emptyList();
        }

        return legalFactsIdList.stream()
                .map(legalFactsId -> {
                    assert legalFactsId.getCategory() != null;
                    return LegalFactsIdV28.builder()
                            .key(legalFactsId.getKey())
                            .category(LegalFactCategoryV28.valueOf(legalFactsId.getCategory().getValue()))
                            .build();
                })
                .toList();
    }

    private static TimelineElementDetails toTimelineElementDetails(TimelineElementDetailsInt detailsInt, String category) {
        if (detailsInt == null) {
            return null;
        }

        TimelineElementDetails details = SmartMapper.mapToClass(detailsInt, TimelineElementDetails.class);
        details.setCategoryType(category);
        return details;
    }

    public static TimelineElementDetailsInt toTimelineElementDetailsInt(TimelineElementDetails details, TimelineElementCategoryInt category) {
        return SmartMapper.mapToClass(details, category.getDetailsJavaClass());
    }

    private static TimelineElementDetailsV28 toTimelineElementDetailsV28(TimelineElementDetails details) {
        return SmartMapper.mapToClass(details, TimelineElementDetailsV28.class);
    }

    private static StatusInfoInternal toStatusInfoInternal(StatusInfo statusInfo) {
        if (statusInfo == null) return null;

        return StatusInfoInternal.builder()
                .actual(statusInfo.getActual())
                .statusChangeTimestamp(statusInfo.getStatusChangeTimestamp())
                .statusChanged(statusInfo.getStatusChanged() != null ? statusInfo.getStatusChanged() : false)
                .build();
    }
}
