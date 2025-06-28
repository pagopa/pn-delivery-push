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
import org.springframework.util.CollectionUtils;

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
                .legalFactsIds(toLegalFactsIdIntList(timelineElement.getLegalFactsIds()))
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
                .notificationStatus(getNotificationStatusV26(source))
                .notificationStatusHistory(getNotificationStatusHistoryElementV26List(source))
                .timeline(getTimelineElementV27List(source))
                .build();
    }

    private static List<TimelineElementV27> getTimelineElementV27List(NotificationHistoryResponse source) {
        List<TimelineElementV27> timeline = null;
        if (source.getTimeline() != null) {
            timeline = source.getTimeline().stream()
                    .map(item -> TimelineElementV27.builder()
                            .elementId(item.getElementId())
                            .timestamp(item.getTimestamp())
                            .legalFactsIds(toLegalFactsIdV20List(item.getLegalFactsIds()))
                            .category(TimelineElementCategoryV27.valueOf(item.getCategory().getValue()))
                            .details(toTimelineElementDetailsV27(item.getDetails()))
                            .notificationSentAt(item.getNotificationSentAt())
                            .ingestionTimestamp(item.getIngestionTimestamp())
                            .eventTimestamp(item.getEventTimestamp())
                            .build())
                    .toList();
        }
        return timeline;
    }

    private static List<NotificationStatusHistoryElementV26> getNotificationStatusHistoryElementV26List(NotificationHistoryResponse source) {
        List<NotificationStatusHistoryElementV26> notificationStatusHistory = null;
        if (source.getNotificationStatusHistory() != null) {
            notificationStatusHistory = source.getNotificationStatusHistory().stream()
                    .map(item -> NotificationStatusHistoryElementV26.builder()
                            .status(NotificationStatusV26.valueOf(item.getStatus().getValue()))
                            .activeFrom(item.getActiveFrom())
                            .relatedTimelineElements(item.getRelatedTimelineElements())
                            .build())
                    .toList();
        }
        return notificationStatusHistory;
    }

    private static NotificationStatusV26 getNotificationStatusV26(NotificationHistoryResponse source) {
        NotificationStatusV26 notificationStatus = null;
        if (source.getNotificationStatus() != null) {
            notificationStatus = NotificationStatusV26.valueOf(source.getNotificationStatus().getValue());
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
                .legalFactsIds(toLegalFactsIdList(timelineElementInternal.getLegalFactsIds()))
                .category(TimelineCategory.valueOf(timelineElementInternal.getCategory().getValue()))
                .details(toTimelineElementDetails(timelineElementInternal.getDetails(), timelineElementInternal.getCategory().getValue()))
                .notificationSentAt(timelineElementInternal.getNotificationSentAt());
    }

    private static List<LegalFactsId> toLegalFactsIdList(List<LegalFactsIdInt> legalFactsIdIntList) {
        if (!CollectionUtils.isEmpty(legalFactsIdIntList)) {
            return legalFactsIdIntList.stream()
                    .map(legalFactsIdInt -> new LegalFactsId()
                            .key(legalFactsIdInt.getKey())
                            .category(LegalFactsId.CategoryEnum.valueOf(legalFactsIdInt.getCategory().getValue())))
                    .toList();
        }
        return Collections.emptyList();
    }

    private static List<LegalFactsIdInt> toLegalFactsIdIntList(List<LegalFactsId> legalFactsIdList) {
        if (!CollectionUtils.isEmpty(legalFactsIdList)) {
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
        return Collections.emptyList();
    }

    private static List<LegalFactsIdV20> toLegalFactsIdV20List(List<LegalFactsId> legalFactsIdList) {
        if (!CollectionUtils.isEmpty(legalFactsIdList)) {
            return legalFactsIdList.stream()
                    .map(legalFactsId -> {
                        assert legalFactsId.getCategory() != null;
                        return LegalFactsIdV20.builder()
                                .key(legalFactsId.getKey())
                                .category(LegalFactCategoryV20.valueOf(legalFactsId.getCategory().getValue()))
                                .build();
                    })
                    .toList();
        }
        return Collections.emptyList();
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

    private static TimelineElementDetailsV27 toTimelineElementDetailsV27(TimelineElementDetails details) {
        return SmartMapper.mapToClass(details, TimelineElementDetailsV27.class);
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
