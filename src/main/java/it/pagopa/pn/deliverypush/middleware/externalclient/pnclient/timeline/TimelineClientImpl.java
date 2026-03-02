package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timeline;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.service.mapper.TimelineServiceMapper;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@CustomLog
@RequiredArgsConstructor
@Component
public class TimelineClientImpl implements TimelineClient {
    private final TimelineControllerApi timelineControllerApi;
    private final TimelineServiceMapper timelineServiceMapper;

    @Override
    public boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        log.logInvokingExternalService(CLIENT_NAME, ADD_TIMELINE_ELEMENT);
        NewTimelineElement newTimelineElement = timelineServiceMapper.getNewTimelineElement(element, notification);
        try {
            timelineControllerApi.addTimelineElement(newTimelineElement);
        } catch (PnHttpResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.SC_CONFLICT) {
                log.warn("Exception idconflict is expected for retry, letting flow continue");
                return true;
            }

            log.error("Error while invoking {}: {}", ADD_TIMELINE_ELEMENT, ex.getMessage(), ex);
            throw ex;
        }
        return false;
    }

    @Override
    public TimelineElementInternal getTimelineElement(String iun, String timelineId, Boolean strongly) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT);

        TimelineElement timelineElement = timelineControllerApi.getTimelineElement(iun, timelineId, strongly);
        return timelineServiceMapper.toTimelineElementInternal(timelineElement);
    }

    @Override
    public TimelineElementDetailsInt getTimelineElementDetails(String iun, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAILS);

        TimelineElementDetails timelineElementDetails = timelineControllerApi.getTimelineElementDetails(iun, timelineId);
        if(timelineElementDetails == null) {
            return null;
        }
        return timelineServiceMapper.toTimelineElementDetailsInt(timelineElementDetails, TimelineElementCategoryInt.valueOf(timelineElementDetails.getCategoryType()));
    }

    @Override
    public TimelineElementDetailsInt getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineElementCategoryInt category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAIL_FOR_SPECIFIC_RECIPIENT);

        TimelineElementDetails timelineElementDetails = timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, TimelineCategory.fromValue(category.name()));
        return timelineServiceMapper.toTimelineElementDetailsInt(timelineElementDetails, category);
    }

    @Override
    public TimelineElementInternal getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineElementCategoryInt category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_FOR_SPECIFIC_RECIPIENT);

        TimelineElement timelineElement = timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineCategory.fromValue(category.name()));
        return timelineServiceMapper.toTimelineElementInternal(timelineElement);
    }

    @Override
    public List<TimelineElementInternal> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE);

        return timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId)
                .stream()
                .filter(element -> TimelineElementCategoryInt.isKnownCategory(element.getCategory().getValue()))
                .map(timelineServiceMapper::toTimelineElementInternal)
                .toList();
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, Integer numberOfRecipients, Instant createdAt) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_AND_STATUS_HISTORY);

        return timelineControllerApi.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
    }

    public LegalFactsResponse getLegalFacts(String iun, Integer recIndex) {
        log.logInvokingExternalService(CLIENT_NAME, GET_LEGAL_FACTS);
        return timelineControllerApi.getLegalFacts(iun, recIndex);
    }
}
