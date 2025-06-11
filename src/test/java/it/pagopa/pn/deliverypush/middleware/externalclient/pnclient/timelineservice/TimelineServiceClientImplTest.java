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
        NewTimelineElement newTimelineElement = Mockito.mock(NewTimelineElement.class);
        PnHttpResponseException exception = new PnHttpResponseException("Conflict", HttpStatus.SC_CONFLICT);

        Mockito.doThrow(exception)
                .when(timelineControllerApi)
                .addTimelineElement(newTimelineElement);

        boolean result = timelineServiceClient.addTimelineElement(newTimelineElement);

        assertTrue(result);
    }

    @Test
    void addTimelineElementReturnsFalseWhenOtherErrorOccurs() {
        NewTimelineElement newTimelineElement = new NewTimelineElement();

        Mockito.doNothing().when(timelineControllerApi).addTimelineElement(Mockito.any());

        boolean result = timelineServiceClient.addTimelineElement(newTimelineElement);

        assertFalse(result);
    }

    @Test
    void addTimelineElement_throwsExceptionOnError() {
        NewTimelineElement newTimelineElement = Mockito.mock(NewTimelineElement.class);
        PnHttpResponseException exception = new PnHttpResponseException("Errore generico", HttpStatus.SC_INTERNAL_SERVER_ERROR);

        Mockito.doThrow(exception)
                .when(timelineControllerApi)
                .addTimelineElement(newTimelineElement);

        PnHttpResponseException thrown = assertThrows(PnHttpResponseException.class, () ->
                timelineServiceClient.addTimelineElement(newTimelineElement)
        );

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, thrown.getStatusCode());
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent_returnsExpectedCounter() {
        String timelineId = "timeline123";
        Long expectedCounter = 42L;

        Mockito.when(timelineControllerApi.retrieveAndIncrementCounterForTimelineEvent(timelineId))
                .thenReturn(expectedCounter);

        Long result = timelineServiceClient.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertEquals(expectedCounter, result);
        Mockito.verify(timelineControllerApi).retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Test
    void getTimelineElement_returnsExpectedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        Boolean strongly = true;
        TimelineElement expectedElement = new TimelineElement();

        Mockito.when(timelineControllerApi.getTimelineElement(iun, timelineId, strongly))
                .thenReturn(expectedElement);

        TimelineElement result = timelineServiceClient.getTimelineElement(iun, timelineId, strongly);

        assertEquals(expectedElement, result);
        Mockito.verify(timelineControllerApi).getTimelineElement(iun, timelineId, strongly);
    }

    @Test
    void getTimelineElementDetails_returnsExpectedDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementDetails expectedDetails = new TimelineElementDetails();

        Mockito.when(timelineControllerApi.getTimelineElementDetails(iun, timelineId))
                .thenReturn(expectedDetails);

        TimelineElementDetails result = timelineServiceClient.getTimelineElementDetails(iun, timelineId);

        assertEquals(expectedDetails, result);
        Mockito.verify(timelineControllerApi).getTimelineElementDetails(iun, timelineId);
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient_returnsExpectedDetails() {
        String iun = "iun123";
        Integer recIndex = 1;
        Boolean confidentialInfoRequired = true;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;
        TimelineElementDetails expectedDetails = new TimelineElementDetails();

        Mockito.when(timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category))
                .thenReturn(expectedDetails);

        TimelineElementDetails result = timelineServiceClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);

        assertEquals(expectedDetails, result);
        Mockito.verify(timelineControllerApi).getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category);
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient_throwsException() {
        String iun = "iun123";
        Integer recIndex = 1;
        Boolean confidentialInfoRequired = true;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;

        Mockito.when(timelineControllerApi.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineElementDetailForSpecificRecipient(iun, recIndex, confidentialInfoRequired, category)
        );
    }

    @Test
    void getTimelineElementForSpecificRecipient_returnsExpectedElement() {
        String iun = "iun123";
        Integer recIndex = 1;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;
        TimelineElement expectedElement = new TimelineElement();

        Mockito.when(timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, category))
                .thenReturn(expectedElement);

        TimelineElement result = timelineServiceClient.getTimelineElementForSpecificRecipient(iun, recIndex, category);

        assertEquals(expectedElement, result);
        Mockito.verify(timelineControllerApi).getTimelineElementForSpecificRecipient(iun, recIndex, category);
    }

    @Test
    void getTimelineElementForSpecificRecipient_throwsException() {
        String iun = "iun123";
        Integer recIndex = 1;
        TimelineCategory category = TimelineCategory.NOTIFICATION_VIEWED;

        Mockito.when(timelineControllerApi.getTimelineElementForSpecificRecipient(iun, recIndex, category))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineElementForSpecificRecipient(iun, recIndex, category)
        );
    }

    @Test
    void getTimeline_returnsExpectedList() {
        String iun = "iun123";
        Boolean confidentialInfoRequired = true;
        Boolean strongly = false;
        String timelineId = "timeline123";
        TimelineElement expectedElement = new TimelineElement();

        Mockito.when(timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId))
                .thenReturn(List.of(expectedElement));

        List<TimelineElement> result = timelineServiceClient.getTimeline(iun, confidentialInfoRequired, strongly, timelineId);

        assertEquals(1, result.size());
        assertEquals(expectedElement, result.get(0));
        Mockito.verify(timelineControllerApi).getTimeline(iun, confidentialInfoRequired, strongly, timelineId);
    }

    @Test
    void getTimeline_throwsException() {
        String iun = "iun123";
        Boolean confidentialInfoRequired = true;
        Boolean strongly = false;
        String timelineId = "timeline123";

        Mockito.when(timelineControllerApi.getTimeline(iun, confidentialInfoRequired, strongly, timelineId))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimeline(iun, confidentialInfoRequired, strongly, timelineId)
        );
    }

    @Test
    void getTimelineAndStatusHistory_returnsExpectedResponse() {
        String iun = "iun123";
        Integer numberOfRecipients = 5;
        Instant createdAt = Instant.now();
        NotificationHistoryResponse expectedResponse = new NotificationHistoryResponse();

        Mockito.when(timelineControllerApi.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt))
                .thenReturn(expectedResponse);

        NotificationHistoryResponse result = timelineServiceClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        assertEquals(expectedResponse, result);
        Mockito.verify(timelineControllerApi).getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
    }

    @Test
    void getTimelineAndStatusHistory_throwsException() {
        String iun = "iun123";
        Integer numberOfRecipients = 5;
        Instant createdAt = Instant.now();

        Mockito.when(timelineControllerApi.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt)
        );
    }

    @Test
    void getSchedulingAnalogDate_returnsExpectedDate() {
        String iun = "iun123";
        Integer recIndex = 1;
        ProbableSchedulingAnalogDate expectedDate = new ProbableSchedulingAnalogDate();

        Mockito.when(timelineControllerApi.getSchedulingAnalogDate(iun, recIndex))
                .thenReturn(expectedDate);

        ProbableSchedulingAnalogDate result = timelineServiceClient.getSchedulingAnalogDate(iun, recIndex);

        assertEquals(expectedDate, result);
        Mockito.verify(timelineControllerApi).getSchedulingAnalogDate(iun, recIndex);
    }

    @Test
    void getSchedulingAnalogDate_throwsException() {
        String iun = "iun123";
        Integer recIndex = 1;

        Mockito.when(timelineControllerApi.getSchedulingAnalogDate(iun, recIndex))
                .thenThrow(new RuntimeException("Errore"));

        assertThrows(RuntimeException.class, () ->
                timelineServiceClient.getSchedulingAnalogDate(iun, recIndex)
        );
    }

}
