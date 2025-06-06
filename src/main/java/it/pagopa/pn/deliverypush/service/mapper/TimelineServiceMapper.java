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

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class TimelineServiceMapper {

    private TimelineServiceMapper() {
    }

    public static InlineObject getInlineObject(TimelineElementInternal timelineElementInternal,
                                               NotificationInt notificationInt) {
        return new InlineObject()
                .timelineElement(toTimelineElement(timelineElementInternal))
                .notificationInfo(toNotificationInfo(notificationInt));
    }

    public static TimelineElementInternal toTimelineElementInternal(TimelineElement timelineElement) {
        if (timelineElement == null) {
            return null;
        }
        TimelineElementCategoryInt category = timelineElement.getCategory() != null ? TimelineElementCategoryInt.valueOf(timelineElement.getCategory().getValue()) : null;
        assert category != null;

        return TimelineElementInternal.builder()
                .iun(timelineElement.getIun())
                .elementId(timelineElement.getElementId())
                .timestamp(timelineElement.getTimestamp())
                .paId(timelineElement.getPaId())
                .legalFactsIds(toLegalFactsIdIntList(timelineElement.getLegalFactsIds()))
                .category(category)
                .details(toTimelineElementDetailsInt(timelineElement, category))
                .statusInfo(toStatusInfoInternal(timelineElement.getStatusInfo()))
                .notificationSentAt(timelineElement.getNotificationSentAt())
                .ingestionTimestamp(timelineElement.getIngestionTimestamp())
                .eventTimestamp(timelineElement.getEventTimestamp())
                .build();
    }

    public static ProbableSchedulingAnalogDateResponse toProbableSchedulingAnalogDateResponse(ProbableSchedulingAnalogDate probableSchedulingAnalogDate) {
        if (probableSchedulingAnalogDate == null) {
            return null;
        }
        return new ProbableSchedulingAnalogDateResponse()
                .iun(probableSchedulingAnalogDate.getIun())
                .recIndex(probableSchedulingAnalogDate.getRecIndex())
                .schedulingAnalogDate(probableSchedulingAnalogDate.getSchedulingAnalogDate());
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

// possiibile alternativa?
//    public static it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse toNotificationHistoryResponse(
//            it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse source) {
//
//        if (source == null) {
//            return null;
//        }
//
//        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusV26 notificationStatus =
//                SmartMapper.mapToClass(source.getNotificationStatus(), it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusV26.class);
//
//        List<it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26> notificationStatusHistory = null;
//        if (source.getNotificationStatusHistory() != null) {
//            notificationStatusHistory = source.getNotificationStatusHistory().stream()
//                    .map(item -> SmartMapper.mapToClass(item, it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26.class))
//                    .toList();
//        }
//
//        List<it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27> timeline = null;
//        if (source.getTimeline() != null) {
//            timeline = source.getTimeline().stream()
//                    .map(item -> SmartMapper.mapToClass(item, it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27.class))
//                    .toList();
//        }
//
//        return it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse.builder()
//                .notificationStatus(notificationStatus)
//                .notificationStatusHistory(notificationStatusHistory)
//                .timeline(timeline)
//                .build();
//    }

    private static List<TimelineElementV27> getTimelineElementV27List(NotificationHistoryResponse source) {
        List<TimelineElementV27> timeline = null;
        if (source.getTimeline() != null) {
            timeline = source.getTimeline().stream()
                    .map(item -> TimelineElementV27.builder()
                            .elementId(item.getElementId())
                            .timestamp(item.getTimestamp())
                            .legalFactsIds(toLegalFactsIdV20List(item.getLegalFactsIds()))
                            .category(item.getCategory() != null ? TimelineElementCategoryV27.valueOf(item.getCategory().getValue()) : null)
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
                .details(toTimelineElementDetails(timelineElementInternal.getDetails()))
                .statusInfo(toStatusInfo(timelineElementInternal.getStatusInfo()))
                .notificationSentAt(timelineElementInternal.getNotificationSentAt())
                .ingestionTimestamp(timelineElementInternal.getIngestionTimestamp())
                .eventTimestamp(timelineElementInternal.getEventTimestamp());
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

    private static StatusInfo toStatusInfo(StatusInfoInternal statusInfoInternal) {
        if (statusInfoInternal == null) return null;
        return new StatusInfo()
                .actual(statusInfoInternal.getActual())
                .statusChangeTimestamp(statusInfoInternal.getStatusChangeTimestamp())
                .statusChanged(statusInfoInternal.isStatusChanged());
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

    private static TimelineElementDetailsInt toTimelineElementDetailsInt(TimelineElement timelineElement, TimelineElementCategoryInt category) {
        return SmartMapper.mapToClass(timelineElement, category.getDetailsJavaClass());
    }

    private static TimelineElementDetails toTimelineElementDetails(TimelineElementDetailsInt details) {
        return SmartMapper.mapToClass(details, TimelineElementDetails.class);
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
