package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusV28;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableSchedulingAnalogDateResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.TimelineServiceMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimelineServiceHttpImplTest {

    @Mock
    private TimelineClient timelineClient;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TimelineServiceHttpImpl timelineServiceHttp;

    @Test
    void addTimelineElement() {
        TimelineElementInternal element = getTimelineElementInternal();
        NotificationInt notification = new NotificationInt();

        Mockito.when(timelineClient.addTimelineElement(Mockito.any(NewTimelineElement.class))).thenReturn(true);

        boolean result = timelineServiceHttp.addTimelineElement(element, notification);

        assertTrue(result);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent() {
        String timelineId = "timeline123";
        Long expectedCounter = 42L;

        Mockito.when(timelineClient.retrieveAndIncrementCounterForTimelineEvent(Mockito.anyString())).thenReturn(expectedCounter);

        Long result = timelineServiceHttp.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertEquals(expectedCounter, result);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEventReturnsNull() {
        String timelineId = "timeline123";
        Mockito.when(timelineClient.retrieveAndIncrementCounterForTimelineEvent(Mockito.anyString())).thenReturn(null);

        Long result = timelineServiceHttp.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        assertNull(result);
    }

    @Test
    void getTimelineElementReturnsMappedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElement timelineElement = new TimelineElement();
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
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

        Mockito.when(timelineClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
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

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
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

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTimelineAndStatusHistory_filtersDiagnosticElements() {
        String iun = "iun123";
        int numberOfRecipients = 1;
        Instant createdAt = Instant.now();

        TimelineElement publicElement = new TimelineElement();
        publicElement.setElementId("publicId");
        publicElement.setCategory(TimelineCategory.SEND_ANALOG_FEEDBACK);

        TimelineElement diagnosticElement = new TimelineElement();
        diagnosticElement.setElementId("diagnosticId");
        diagnosticElement.setCategory(TimelineCategory.VALIDATE_F24_REQUEST);

        NotificationStatusHistoryElement statusHistory = new NotificationStatusHistoryElement();
        statusHistory.setRelatedTimelineElements(Arrays.asList("publicId", "diagnosticId"));

        NotificationHistoryResponse clientResponse = new NotificationHistoryResponse();
        clientResponse.setTimeline(Arrays.asList(publicElement, diagnosticElement));
        clientResponse.setNotificationStatusHistory(Collections.singletonList(statusHistory));

        when(timelineClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt)).thenReturn(clientResponse);

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse mappedResponse =
                new it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse();
        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27 publicElementDto =
                new it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27();
        publicElementDto.setElementId("publicId");
        mappedResponse.setTimeline(Collections.singletonList(publicElementDto));
        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26 statusHistoryDto =
                new it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26();
        statusHistoryDto.setRelatedTimelineElements(Collections.singletonList("publicId"));
        mappedResponse.setNotificationStatusHistory(Collections.singletonList(statusHistoryDto));

        try (MockedStatic<TimelineServiceMapper> mockedMapper = Mockito.mockStatic(TimelineServiceMapper.class)) {
            mockedMapper.when(() -> TimelineServiceMapper.toNotificationHistoryResponseDto(clientResponse))
                    .thenReturn(mappedResponse);

            it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result =
                    timelineServiceHttp.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

            // Verifica che solo l'elemento pubblico sia presente
            assertEquals(1, result.getTimeline().size());
            assertEquals("publicId", result.getTimeline().get(0).getElementId());
            // Verifica che solo l'id pubblico sia rimasto nei related
            assertEquals(1, result.getNotificationStatusHistory().get(0).getRelatedTimelineElements().size());
            assertEquals("publicId", result.getNotificationStatusHistory().get(0).getRelatedTimelineElements().get(0));
        }
    }


    @Test
    void getTimelineElementDetailsReturnsMappedDetails() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementDetails timelineElementDetails = new TimelineElementDetails();
        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;
        TimelineElementDetailsInt mappedDetails = Mockito.mock(TimelineElementDetailsInt.class);

        Mockito.when(timelineClient.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString()))
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

        Mockito.when(timelineClient.getTimelineElementDetailForSpecificRecipient(
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

        Mockito.when(timelineClient.getTimelineElementForSpecificRecipient(
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

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(true), Mockito.isNull()))
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

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(true), Mockito.isNull()))
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

        Mockito.when(timelineClient.getTimeline(
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

        Mockito.when(timelineClient.getTimeline(
                iun,
                confidentialInfoRequired,
                false,
                timelineId
        )).thenReturn(null);

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);

        assertTrue(result.isEmpty());
    }

    @Test
    void getSchedulingAnalogDateOKTest() {
        final String iun = "iun1";
        final String recipientId = "cxId";

        String timelineElementIdExpected = TimelineEventId.PROBABLE_SCHEDULING_ANALOG_DATE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(0)
                .build());
        Instant schedulingDate = Instant.now();
        TimelineElementInternal timelineElementExpected = TimelineElementInternal.builder()
                .elementId(timelineElementIdExpected)
                .timestamp(schedulingDate)
                .category(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE)
                .details(ProbableDateAnalogWorkflowDetailsInt.builder()
                        .schedulingAnalogDate(schedulingDate)
                        .recIndex(0)
                        .build())
                .build();

        Mockito.when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(NotificationInt.builder()
                        .recipients(List.of(NotificationRecipientInt.builder()
                                .internalId(recipientId)
                                .build()))
                        .build()));

        TimelineElementDetails details=new TimelineElementDetails();
        details.setSchedulingAnalogDate(schedulingDate);
        details.setRecIndex(0);
        details.setCategoryType(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE.name());
        Mockito.when(timelineClient.getTimelineElementDetailForSpecificRecipient(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(details);
        ProbableSchedulingAnalogDateResponse schedulingAnalogDateActual = timelineServiceHttp.getSchedulingAnalogDate(iun, recipientId).block();

        assertNotNull(schedulingAnalogDateActual);
        assertThat(schedulingAnalogDateActual.getSchedulingAnalogDate())
                .isEqualTo(((ProbableDateAnalogWorkflowDetailsInt) timelineElementExpected.getDetails()).getSchedulingAnalogDate());

        assertThat(schedulingAnalogDateActual.getIun()).isEqualTo(iun);
        assertThat(schedulingAnalogDateActual.getRecIndex()).isZero();
    }

    @Test
    void getSchedulingAnalogDateNotFoundTest() {
        final String iun = "iun1";
        final String recipientId = "cxId";

        Mockito.when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(NotificationInt.builder()
                        .recipients(List.of(NotificationRecipientInt.builder()
                                .internalId(recipientId)
                                .build()))
                        .build()));

        Mockito.when(timelineClient.getTimelineElementDetailForSpecificRecipient(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
                )
                .thenReturn(null);

        Executable executable = () -> timelineServiceHttp.getSchedulingAnalogDate(iun, recipientId).block();
        Assertions.assertThrows(PnNotFoundException.class, executable);
    }

    @Test
    void getTimelineAndStatusHistory_ReturnsMappedResponse() {
        String iun = "iunTest";
        int numberOfRecipients = 2;
        Instant createdAt = Instant.now();

        it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse clientResponse =
                new NotificationHistoryResponse();

        // BUILDING THE TIMELINE ELEMENTS
        String elementId1 = "elementId1";
        List<TimelineElement> setTimelineElement = new ArrayList<>();
        Instant t = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        TimelineElement elementDiagnostic = new TimelineElement();
        elementDiagnostic.setIun(iun);
        elementDiagnostic.setElementId(elementId1 + "DIAGNOSTIC");
        elementDiagnostic.setTimestamp(t);
        elementDiagnostic.setCategory(TimelineCategory.VALIDATE_F24_REQUEST);
        setTimelineElement.add(elementDiagnostic);
        TimelineElement elementFeedback = new TimelineElement();
        elementFeedback.setIun(iun);
        elementFeedback.setElementId(elementId1 + "FEEDBACK");
        elementFeedback.setTimestamp(t);
        elementFeedback.setCategory(TimelineCategory.SEND_ANALOG_FEEDBACK);
        setTimelineElement.add(elementFeedback);
        TimelineElement elementProg = new TimelineElement();
        elementProg.setIun(iun);
        elementProg.setElementId(elementId1 + "PROGRESS");
        elementProg.setTimestamp(t);
        elementProg.setCategory(TimelineCategory.SEND_ANALOG_PROGRESS);
        setTimelineElement.add(elementProg);


        clientResponse.setTimeline(setTimelineElement);

        // BUILDING THE NOTIFICATION STATUS HISTORY ELEMENTS

        Instant activeFromInValidation = Instant.now();
        NotificationStatusHistoryElement inValidationElement = new NotificationStatusHistoryElement();
        inValidationElement.status(NotificationStatus.IN_VALIDATION);
        inValidationElement.activeFrom(activeFromInValidation);

        Instant activeFromAccepted = activeFromInValidation.plus(Duration.ofDays(1));

        NotificationStatusHistoryElement acceptedElementElement = new NotificationStatusHistoryElement();
        acceptedElementElement.status(NotificationStatus.ACCEPTED);
        acceptedElementElement.activeFrom(activeFromAccepted);

        Instant activeFromDelivering = activeFromAccepted.plus(Duration.ofDays(1));

        NotificationStatusHistoryElement deliveringElement = new NotificationStatusHistoryElement();
        deliveringElement.status(NotificationStatus.DELIVERING);
        deliveringElement.activeFrom(activeFromDelivering);


        List<NotificationStatusHistoryElement> notificationStatusHistoryElements = new ArrayList<>(List.of(inValidationElement, acceptedElementElement, deliveringElement));
        clientResponse.setNotificationStatusHistory(notificationStatusHistoryElements);

        // BUILDING THE NOTIFICATION STATUS
        clientResponse.setNotificationStatus(NotificationStatus.DELIVERING);


        Mockito.when(timelineClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt))
                .thenReturn(clientResponse);

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result =
                timelineServiceHttp.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        assertNotNull(result);
        assertEquals(2, result.getTimeline().size()); // Dovrebbe escludere l'elemento di diagnostica e dunque da 3 elementi di partenza a 2 elementi finali
        assertEquals("elementId1FEEDBACK", result.getTimeline().get(0).getElementId());
        assertEquals("elementId1PROGRESS", result.getTimeline().get(1).getElementId());
        assertEquals(NotificationStatusV28.DELIVERING, result.getNotificationStatus());

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
