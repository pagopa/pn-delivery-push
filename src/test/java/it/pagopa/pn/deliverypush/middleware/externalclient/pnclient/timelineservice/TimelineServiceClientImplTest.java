package it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timelineservice;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.api.TimelineControllerApi;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TimelineServiceClientImplTest {

    @Mock
    private TimelineControllerApi timelineControllerApi;

    @InjectMocks
    private TimelineServiceClientImpl timelineServiceClient;

    @Test
    void addTimelineElementReturnsTrueWhenConflictOccurs() {
        NewTimelineElement newTimelineElement = new NewTimelineElement();
        PnHttpResponseException exception = new PnHttpResponseException("Conflict", HttpStatus.SC_CONFLICT);

        Mockito.when(timelineControllerApi.addTimelineElementWithHttpInfo(newTimelineElement))
                .thenThrow(exception);

        Boolean result = timelineServiceClient.addTimelineElement(newTimelineElement);

        assertTrue(result);
    }

    @Test
    void addTimelineElementReturnsFalseWhenOtherErrorOccurs() {
        NewTimelineElement newTimelineElement = new NewTimelineElement();
        PnHttpResponseException exception = new PnHttpResponseException("Bad Request", HttpStatus.SC_BAD_REQUEST);

        Mockito.when(timelineControllerApi.addTimelineElementWithHttpInfo(newTimelineElement))
                .thenThrow(exception);

        Boolean result = timelineServiceClient.addTimelineElement(newTimelineElement);

        assertFalse(result);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent() {
        String timelineId = "timeline123";
        Long expectedCounter = 42L;

        Mockito.when(timelineControllerApi.retrieveAndIncrementCounterForTimelineEventWithHttpInfo(timelineId))
                .thenReturn(ResponseEntity.ok(expectedCounter));

        Long result = timelineServiceClient.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertEquals(expectedCounter, result);
    }

    @Test
    void getTimelineElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        Boolean strongly = true;
        TimelineElement expectedElement = new TimelineElement();

        Mockito.when(timelineControllerApi.getTimelineElementWithHttpInfo(iun, timelineId, strongly))
                .thenReturn(ResponseEntity.ok(expectedElement));

        TimelineElement result = timelineServiceClient.getTimelineElement(iun, timelineId, strongly);

        assertEquals(expectedElement, result);
    }

    @Test
    void getTimelineElementDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementDetails expectedDetails = new TimelineElementDetails();

        Mockito.when(timelineControllerApi.getTimelineElementDetailsWithHttpInfo(iun, timelineId))
                .thenReturn(ResponseEntity.ok(expectedDetails));

        TimelineElementDetails result = timelineServiceClient.getTimelineElementDetails(iun, timelineId);

        assertEquals(expectedDetails, result);
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient() {
        String iun = "iun123";
        Integer recIndex = 1;
        Boolean confidentialInfoRequired = true;
        TimelineElementDetails expectedDetails = new TimelineElementDetails();

        Mockito.when(timelineControllerApi.getTimelineElementDetailForSpecificRecipientWithHttpInfo(iun, recIndex, confidentialInfoRequired, TimelineCategory.NOTIFICATION_VIEWED))
                .thenReturn(ResponseEntity.ok(expectedDetails));

        TimelineElementDetails result = timelineServiceClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, TimelineCategory.NOTIFICATION_VIEWED);

        assertEquals(expectedDetails, result);
    }

    @Test
    void getTimelineElementForSpecificRecipient() {
        String iun = "iun123";
        Integer recIndex = 1;
        TimelineElement expectedElement = new TimelineElement();

        Mockito.when(timelineControllerApi.getTimelineElementForSpecificRecipientWithHttpInfo(iun, recIndex, TimelineCategory.NOTIFICATION_VIEWED))
                .thenReturn(ResponseEntity.ok(expectedElement));

        TimelineElement result = timelineServiceClient.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineCategory.NOTIFICATION_VIEWED);

        assertEquals(expectedElement, result);
    }

    @Test
    void getTimeline() {
        String iun = "iun123";
        Boolean confidentialInfoRequired = true;
        Boolean strongly = false;
        String timelineId = "timeline123";
        TimelineElement expectedElement = new TimelineElement();

        Mockito.when(timelineControllerApi.getTimelineWithHttpInfo(iun, confidentialInfoRequired, strongly, timelineId))
                .thenReturn(ResponseEntity.ok(List.of(expectedElement)));

        List<TimelineElement> result = timelineServiceClient.getTimeline(iun, confidentialInfoRequired, strongly, timelineId);

        assertEquals(1, result.size());
        assertEquals(expectedElement, result.get(0));
    }

    @Test
    void getTimelineAndStatusHistory() {
        String iun = "iun123";
        Integer numberOfRecipients = 5;
        Instant createdAt = Instant.now();
        NotificationHistoryResponse expectedResponse = new NotificationHistoryResponse();

        Mockito.when(timelineControllerApi.getTimelineAndStatusHistoryWithHttpInfo(iun, numberOfRecipients, createdAt))
                .thenReturn(ResponseEntity.ok(expectedResponse));

        NotificationHistoryResponse result = timelineServiceClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        assertEquals(expectedResponse, result);
    }

    @Test
    void getSchedulingAnalogDate() {
        String iun = "iun123";
        Integer recIndex = 1;
        ProbableSchedulingAnalogDate expectedDate = new ProbableSchedulingAnalogDate();

        Mockito.when(timelineControllerApi.getSchedulingAnalogDateWithHttpInfo(iun, recIndex))
                .thenReturn(ResponseEntity.ok(expectedDate));

        ProbableSchedulingAnalogDate result = timelineServiceClient.getSchedulingAnalogDate(iun, recIndex);

        assertEquals(expectedDate, result);
    }
}
