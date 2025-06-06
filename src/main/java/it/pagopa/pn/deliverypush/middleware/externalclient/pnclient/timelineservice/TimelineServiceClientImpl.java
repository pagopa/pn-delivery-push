package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timelineservice;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@CustomLog
@RequiredArgsConstructor
@Component
public class TimelineServiceClientImpl implements TimelineServiceClient {
    private final TimelineControllerApi timelineControllerApi;

    @Override
    public Boolean addTimelineElement(InlineObject inlineObject) {
        log.logInvokingExternalService(CLIENT_NAME, ADD_TIMELINE_ELEMENT);

        ResponseEntity<Boolean> resp;
        try {
            resp = timelineControllerApi.addTimelineElementWithHttpInfo(inlineObject);
        } catch (PnHttpResponseException ex) {
            log.error("Error while invoking {}: {}", ADD_TIMELINE_ELEMENT, ex.getMessage(), ex);
            return ex.getStatusCode() == HttpStatus.SC_CONFLICT;
        }
        return resp.getBody();
    }

    @Override
    public Long retrieveAndIncrementCounterForTimelineEvent(String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, RETRIEVE_AND_INCREMENT_COUNTER_FOR_TIMELINE_EVENT);

        ResponseEntity<Long> resp = timelineControllerApi.retrieveAndIncrementCounterForTimelineEventWithHttpInfo(timelineId);
        return resp.getBody();
    }

    @Override
    public TimelineElement getTimelineElement(String iun, String timelineId, Boolean strongly) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT);

        ResponseEntity<TimelineElement> resp = timelineControllerApi.getTimelineElementWithHttpInfo(iun, timelineId, strongly);
        return resp.getBody();
    }

    @Override
    public TimelineElementDetails getTimelineElementDetails(String iun, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAILS);

        ResponseEntity<TimelineElementDetails> resp = timelineControllerApi.getTimelineElementDetailsWithHttpInfo(iun, timelineId);
        return resp.getBody();
    }

    @Override
    public TimelineElementDetails getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineCategory category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_DETAIL_FOR_SPECIFIC_RECIPIENT);

        ResponseEntity<TimelineElementDetails> resp = timelineControllerApi.getTimelineElementDetailForSpecificRecipientWithHttpInfo(iun, recIndex, confidentialInfoRequired, category);
        return resp.getBody();
    }

    @Override
    public TimelineElement getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineCategory category) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_ELEMENT_FOR_SPECIFIC_RECIPIENT);

        ResponseEntity<TimelineElement> resp = timelineControllerApi.getTimelineElementForSpecificRecipientWithHttpInfo(iun, recIndex, category);
        return resp.getBody();
    }

    @Override
    public List<TimelineElement> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE);

        ResponseEntity<List<TimelineElement>> resp = timelineControllerApi.getTimelineWithHttpInfo(iun, confidentialInfoRequired, strongly, timelineId);
        return resp.getBody();
    }

    @Override
    public NotificationHistoryResponse getTimelineAndStatusHistory(String iun, Integer numberOfRecipients, Instant createdAt) {
        log.logInvokingExternalService(CLIENT_NAME, GET_TIMELINE_AND_STATUS_HISTORY);

        ResponseEntity<NotificationHistoryResponse> resp = timelineControllerApi.getTimelineAndStatusHistoryWithHttpInfo(iun, numberOfRecipients, createdAt);
        return resp.getBody();
    }

    @Override
    public ProbableSchedulingAnalogDate getSchedulingAnalogDate(String iun, Integer recIndex) {
        log.logInvokingExternalService(CLIENT_NAME, GET_SCHEDULING_ANALOG_DATE);

        ResponseEntity<ProbableSchedulingAnalogDate> resp = timelineControllerApi.getSchedulingAnalogDateWithHttpInfo(iun, recIndex);
        return resp.getBody();
    }
}
