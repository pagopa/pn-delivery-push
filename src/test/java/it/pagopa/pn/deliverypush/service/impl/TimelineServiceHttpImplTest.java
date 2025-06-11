package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableSchedulingAnalogDateResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timelineservice.TimelineServiceClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.TimelineServiceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimelineServiceHttpImplTest {

    @Mock
    private TimelineServiceClient timelineServiceClient;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TimelineServiceHttpImpl timelineServiceHttp;

    @Test
    void addTimelineElement() {
        TimelineElementInternal element = getTimelineElementInternal();
        NotificationInt notification = new NotificationInt();

        Mockito.when(timelineServiceClient.addTimelineElement(Mockito.any(NewTimelineElement.class))).thenReturn(true);

        boolean result = timelineServiceHttp.addTimelineElement(element, notification);

        assertTrue(result);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent() {
        String timelineId = "timeline123";
        Long expectedCounter = 42L;

        Mockito.when(timelineServiceClient.retrieveAndIncrementCounterForTimelineEvent(Mockito.anyString())).thenReturn(expectedCounter);

        Long result = timelineServiceHttp.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertEquals(expectedCounter, result);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEventReturnsNull() {
        String timelineId = "timeline123";
        Mockito.when(timelineServiceClient.retrieveAndIncrementCounterForTimelineEvent(Mockito.anyString())).thenReturn(null);

        Long result = timelineServiceHttp.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertNull(result);
    }

    @Test
    void getTimelineElementReturnsMappedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineServiceClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(timelineElement);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(expectedElement);

            Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElement(iun, timelineId);

            assertTrue(result.isPresent());
            assertEquals(expectedElement, result.get());
        }
    }

    @Test
    void getTimelineElementStronglyReturnsMappedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineServiceClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(timelineElement);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(expectedElement);

            Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElementStrongly(iun, timelineId);

            assertTrue(result.isPresent());
            assertEquals(expectedElement, result.get());
        }
    }

    @Test
    void getTimelineReturnsMappedSetWhenClientReturnsElements() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal mappedElement = new TimelineElementInternal();

        Mockito.when(timelineServiceClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(Collections.singletonList(timelineElement));

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(mappedElement);

            Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);

            assertEquals(1, result.size());
            assertTrue(result.contains(mappedElement));
        }
    }

    @Test
    void getTimelineReturnsEmptySetWhenClientReturnsNull() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineServiceClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }


    @Test
    void getTimelineElementDetailsReturnsMappedDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementDetails timelineElementDetails = new TimelineElementDetails();
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetailsInt mappedDetails = Mockito.mock(TimelineElementDetailsInt.class);

        Mockito.when(timelineServiceClient.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(timelineElementDetails);

        // Simula il categoryType nel TimelineElementDetails
        timelineElementDetails.setCategoryType(category.name());

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementDetailsInt(
                    Mockito.any(), Mockito.any()))
                    .thenReturn(mappedDetails);

            Optional<TimelineElementDetailsInt> result = timelineServiceHttp.getTimelineElementDetails(iun, timelineId, TimelineElementDetailsInt.class);

            assertTrue(result.isPresent());
            assertEquals(mappedDetails, result.get());
        }
    }

    @Test
    void getTimelineElementDetailForSpecificRecipientReturnsMappedDetails() {
        String iun = "iun123";
        int recIndex = 0;
        boolean confidentialInfoRequired = true;
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetails timelineElementDetails = new TimelineElementDetails();
        timelineElementDetails.setCategoryType(category.name());

        Mockito.when(timelineServiceClient.getTimelineElementDetailForSpecificRecipient(
                iun,
                recIndex,
                confidentialInfoRequired,
                TimelineCategory.fromValue(category.getValue())
        )).thenReturn(timelineElementDetails);

        TimelineElementDetailsInt mappedDetails = Mockito.mock(TimelineElementDetailsInt.class);
        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementDetailsInt(
                    Mockito.eq(timelineElementDetails),
                    Mockito.eq(TimelineElementCategoryInt.valueOf(timelineElementDetails.getCategoryType()))
            )).thenReturn(mappedDetails);

            Optional<TimelineElementDetailsInt> result = timelineServiceHttp.getTimelineElementDetailForSpecificRecipient(
                    iun, recIndex, confidentialInfoRequired, category, TimelineElementDetailsInt.class);

            assertTrue(result.isPresent());
            assertEquals(mappedDetails, result.get());
        }
    }

    @Test
    void getTimelineElementForSpecificRecipientReturnsMappedElement() {
        String iun = "iun123";
        int recIndex = 1;
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineServiceClient.getTimelineElementForSpecificRecipient(
                iun,
                recIndex,
                TimelineCategory.fromValue(category.getValue())
        )).thenReturn(timelineElement);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(expectedElement);

            Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElementForSpecificRecipient(iun, recIndex, category);

            assertTrue(result.isPresent());
            assertEquals(expectedElement, result.get());
        }
    }

    @Test
    void getTimelineStronglyReturnsMappedSetWhenClientReturnsElements() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal mappedElement = new TimelineElementInternal();

        Mockito.when(timelineServiceClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(true), Mockito.isNull()))
                .thenReturn(Collections.singletonList(timelineElement));

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(mappedElement);

            Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineStrongly(iun, confidentialInfoRequired);

            assertEquals(1, result.size());
            assertTrue(result.contains(mappedElement));
        }
    }

    @Test
    void getTimelineStronglyReturnsEmptySetWhenClientReturnsNull() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineServiceClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(true), Mockito.isNull()))
                .thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineStrongly(iun, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTimelineByIunTimelineIdReturnsMappedSet() {
        String iun = "iunTest";
        String timelineId = "timelineIdTest";
        boolean confidentialInfoRequired = true;
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal mappedElement = new TimelineElementInternal();

        Mockito.when(timelineServiceClient.getTimeline(
                iun,
                confidentialInfoRequired,
                false,
                timelineId
        )).thenReturn(Collections.singletonList(timelineElement));

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toTimelineElementInternal(Mockito.any()))
                    .thenReturn(mappedElement);

            Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);

            assertEquals(1, result.size());
            assertTrue(result.contains(mappedElement));
        }
    }

    @Test
    void getTimelineByIunTimelineIdReturnsEmptySetWhenClientReturnsNull() {
        String iun = "iunTest";
        String timelineId = "timelineIdTest";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineServiceClient.getTimeline(
                iun,
                confidentialInfoRequired,
                false,
                timelineId
        )).thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }

    @Test
    void getSchedulingAnalogDate_ReturnsMappedResponse() {
        String iun = "iunTest";
        String recipientId = "recipient123";
        int recIndex = 0;

        ProbableSchedulingAnalogDate probableSchedulingAnalogDate = new ProbableSchedulingAnalogDate();
        ProbableSchedulingAnalogDateResponse mappedResponse = new ProbableSchedulingAnalogDateResponse();

        TimelineServiceClient serviceClient = Mockito.mock(TimelineServiceClient.class);

        Mockito.when(notificationService.getNotificationByIunReactive(iun)).thenReturn(
                Mono.just(
                        NotificationInt.builder()
                                .recipients(List.of(NotificationRecipientInt.builder()
                                        .internalId(recipientId)
                                        .build()
                                ))
                                .build()
                ));
        Mockito.when(serviceClient.getSchedulingAnalogDate(iun, recIndex)).thenReturn(probableSchedulingAnalogDate);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toProbableSchedulingAnalogDateResponse(probableSchedulingAnalogDate))
                    .thenReturn(mappedResponse);

            TimelineServiceHttpImpl service = new TimelineServiceHttpImpl(serviceClient, notificationService);

            ProbableSchedulingAnalogDateResponse result = service.getSchedulingAnalogDate(iun, recipientId).block();

            assertEquals(mappedResponse, result);
        }
    }

    @Test
    void getTimelineAndStatusHistory_ReturnsMappedResponse() {
        String iun = "iunTest";
        int numberOfRecipients = 2;
        Instant createdAt = Instant.now();

        it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse clientResponse =
                Mockito.mock(it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse.class);
        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse expectedResponse =
                Mockito.mock(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse.class);

        Mockito.when(timelineServiceClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt))
                .thenReturn(clientResponse);

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toNotificationHistoryResponseDto(clientResponse))
                    .thenReturn(expectedResponse);

            it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result =
                    timelineServiceHttp.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

            assertEquals(expectedResponse, result);
        }
    }

    private TimelineElementInternal getTimelineElementInternal() {
        Instant timestamp = Instant.ofEpochMilli(1633072800000L);
        TimelineElementInternal element = new TimelineElementInternal();
        element.setIun("iun123");
        element.setElementId("element123");
        element.setTimestamp(timestamp); // Example timestamp
        element.setPaId("pa123");
        element.setLegalFactsIds(new ArrayList<>());
        element.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED);
        element.setDetails(SendAnalogFeedbackDetailsInt.builder().build());
        element.setStatusInfo(StatusInfoInternal.builder().actual("actual").build());
        element.setNotificationSentAt(timestamp);
        element.setIngestionTimestamp(timestamp);
        element.setEventTimestamp(timestamp);
        return element;
    }
}
