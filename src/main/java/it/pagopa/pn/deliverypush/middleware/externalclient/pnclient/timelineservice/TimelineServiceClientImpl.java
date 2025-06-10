package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timelineservice;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@CustomLog
@RequiredArgsConstructor
@Component
public class TimelineServiceClientImpl implements TimelineServiceClient {
    private final TimelineControllerApi timelineControllerApi;

    @Override
    public Boolean addTimelineElement(NewTimelineElement newTimelineElement) {
        log.logInvokingExternalService(CLIENT_NAME, ADD_TIMELINE_ELEMENT);

        try {
            timelineControllerApi.addTimelineElement(newTimelineElement);
        } catch (PnHttpResponseException ex) {
            log.error("Error while invoking {}: {}", ADD_TIMELINE_ELEMENT, ex.getMessage(), ex);
            if (ex.getStatusCode() == HttpStatus.SC_CONFLICT) {
                return true;
            } else {
                throw ex;
            }
        }
        return false;
    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, RETRIEVE_AND_INCREMENT_COUNTER_FOR_TIMELINE_EVENT);

        return timelineControllerApi.retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Override
    public TimelineElement getTimelineElement(String iun, String timelineId, Boolean strongly) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT);

        return timelineControllerApi.getTimelineElement(iun, timelineId, strongly);
    }

    @Override
    public TimelineElementDetails getTimelineElementDetails(String iun, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAILS);

        return timelineControllerApi.getTimelineElementDetails(iun, timelineId);
    }

    @Override
    public TimelineElementDetails getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineCategory category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAIL_FOR_SPECIFIC_RECIPIENT);

        return timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);
    }

    @Override
    public TimelineElement getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineCategory category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_FOR_SPECIFIC_RECIPIENT);

        return timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, category);
    }

    @Override
    public List<TimelineElement> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE);

        return timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId);
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, Integer numberOfRecipients, Instant createdAt) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_AND_STATUS_HISTORY);

        return timelineControllerApi.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
    }

    @Override
    public ProbableSchedulingAnalogDate getSchedulingAnalogDate(String iun, Integer recIndex) {
        log.logInvokingExternalService(CLIENT_NAME, GET_SCHEDULING_ANALOG_DATE);

        return timelineControllerApi.getSchedulingAnalogDate(iun, recIndex);
    }
}
