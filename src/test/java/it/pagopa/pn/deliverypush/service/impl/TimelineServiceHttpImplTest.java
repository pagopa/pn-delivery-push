package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdIntWithRecIndex;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timeline.TimelineClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.TimelineServiceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimelineServiceHttpImplTest {

    @Mock
    private TimelineClient timelineClient;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TimelineServiceMapper timelineServiceMapper;

    @InjectMocks
    private TimelineServiceHttpImpl timelineServiceHttp;

    @Test
    void addTimelineElement() {
        TimelineElementInternal element = getTimelineElementInternal();
        NotificationInt notification = new NotificationInt();

        Mockito.when(timelineClient.addTimelineElement(element, notification)).thenReturn(true);

        boolean result = timelineServiceHttp.addTimelineElement(element, notification);

        assertTrue(result);
    }

    @Test
    void getTimelineElementReturnsMappedElement() {
        String iun = "iun123";
        String timelineId = "timeline123";
        TimelineElementInternal expectedElement = new TimelineElementInternal();

        Mockito.when(timelineClient.getTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(expectedElement);

        Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElement(iun, timelineId);

        assertTrue(result.isPresent());
        assertEquals(expectedElement, result.get());
    }

    @Test
    void getTimelineReturnsMappedSetWhenClientReturnsElements() {
        String iun = "iun123";
        boolean confidentialInfoRequired = true;

        Mockito.when(timelineClient.getTimeline(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(List.of(new TimelineElementInternal()));

        Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);

        assertEquals(1, result.size());
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
        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV28 publicElementDto =
                new it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV28();
        publicElementDto.setElementId("publicId");
        mappedResponse.setTimeline(Collections.singletonList(publicElementDto));
        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26 statusHistoryDto =
                new it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26();
        statusHistoryDto.setRelatedTimelineElements(Collections.singletonList("publicId"));
        mappedResponse.setNotificationStatusHistory(Collections.singletonList(statusHistoryDto));

        Mockito.when(timelineServiceMapper.toNotificationHistoryResponseDto(clientResponse))
                .thenReturn(mappedResponse);

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result =
                timelineServiceHttp.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        // Verifica che solo l'elemento pubblico sia presente
        assertEquals(1, result.getTimeline().size());
        assertEquals("publicId", result.getTimeline().getFirst().getElementId());
        // Verifica che solo l'id pubblico sia rimasto nei related
        assertEquals(1, result.getNotificationStatusHistory().getFirst().getRelatedTimelineElements().size());
        assertEquals("publicId", result.getNotificationStatusHistory().getFirst().getRelatedTimelineElements().getFirst());
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

        ArgumentCaptor<NotificationHistoryResponse> clientHistoryResponseCaptor = ArgumentCaptor.forClass(NotificationHistoryResponse.class);

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse mappedResponse =
                new it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse();
        Mockito.when(timelineServiceMapper.toNotificationHistoryResponseDto(Mockito.any()))
                .thenReturn(mappedResponse);

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result =
                timelineServiceHttp.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        assertNotNull(result);
        // Verifico che gli elementi di diagnostica siano stati rimossi prima del mapping finale
        Mockito.verify(timelineServiceMapper).toNotificationHistoryResponseDto(clientHistoryResponseCaptor.capture());

        NotificationHistoryResponse capturedClientResponse = clientHistoryResponseCaptor.getValue();
        assertNotNull(capturedClientResponse.getTimeline());
        assertEquals(2, capturedClientResponse.getTimeline().size()); // Dovrebbe escludere l'elemento di diagnostica e dunque da 3 elementi di partenza a 2 elementi finali
        assertEquals("elementId1FEEDBACK", capturedClientResponse.getTimeline().get(0).getElementId());
        assertEquals("elementId1PROGRESS", capturedClientResponse.getTimeline().get(1).getElementId());
        assertEquals(NotificationStatus.DELIVERING, capturedClientResponse.getNotificationStatus());

    }

    @Test
    void isNotificationRefused_whenRequestRefusedIsPresent_returnsTrue() {
        // Arrange
        String iun = "testIun123";
        RequestRefusedResponse requestRefusedResponse = new RequestRefusedResponse();

        when(timelineClient.getRequestRefused(iun)).thenReturn(Optional.of(requestRefusedResponse));

        // Act
        boolean result = timelineServiceHttp.isNotificationRefused(iun);

        // Assert
        assertTrue(result);
        Mockito.verify(timelineClient).getRequestRefused(iun);
    }

    @Test
    void isNotificationRefused_whenRequestRefusedIsNotPresent_returnsFalse() {
        // Arrange
        String iun = "testIun123";

        when(timelineClient.getRequestRefused(iun)).thenReturn(Optional.empty());

        // Act
        boolean result = timelineServiceHttp.isNotificationRefused(iun);

        // Assert
        assertFalse(result);
        Mockito.verify(timelineClient).getRequestRefused(iun);
    }

    @Test
    void getRecipientAARUrl_whenAarExists_returnsUrl() {
        // Arrange
        String iun = "testIun456";
        int recIndex = 0;
        String expectedUrl = "https://example.com/aar/document.pdf";

        AarResponse aarResponse = new AarResponse();
        aarResponse.setUrl(expectedUrl);

        when(timelineClient.getAarForRecipient(iun, recIndex)).thenReturn(Optional.of(aarResponse));

        // Act
        Optional<String> result = timelineServiceHttp.getRecipientAARUrl(iun, recIndex);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedUrl, result.get());
        Mockito.verify(timelineClient).getAarForRecipient(iun, recIndex);
    }

    @Test
    void getRecipientAARUrl_whenAarDoesNotExist_returnsEmpty() {
        // Arrange
        String iun = "testIun456";
        int recIndex = 0;

        when(timelineClient.getAarForRecipient(iun, recIndex)).thenReturn(Optional.empty());

        // Act
        Optional<String> result = timelineServiceHttp.getRecipientAARUrl(iun, recIndex);

        // Assert
        assertFalse(result.isPresent());
        Mockito.verify(timelineClient).getAarForRecipient(iun, recIndex);
    }

    @Test
    void getRecipientAARUrl_whenAarExistsButUrlIsNull_returnsEmpty() {
        // Arrange
        String iun = "testIun789";
        int recIndex = 1;

        AarResponse aarResponse = new AarResponse();
        aarResponse.setUrl(null);

        when(timelineClient.getAarForRecipient(iun, recIndex)).thenReturn(Optional.of(aarResponse));

        // Act
        Optional<String> result = timelineServiceHttp.getRecipientAARUrl(iun, recIndex);

        // Assert
        assertFalse(result.isPresent());
        Mockito.verify(timelineClient).getAarForRecipient(iun, recIndex);
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
        element.setDetails(NotificationViewedDetailsInt.builder().build());
        element.setStatusInfo(StatusInfoInternal.builder().actual("actual").build());
        element.setNotificationSentAt(timestamp);
        element.setIngestionTimestamp(timestamp);
        element.setEventTimestamp(timestamp);
        return element;
    }

    @Test
    void getLegalFacts_returnsMappedLegalFacts() {
        String iun = "IUN";
        Integer recIndex = 1;

        LegalFactWithRecIndex fact = new LegalFactWithRecIndex();
        fact.setKey("key1");
        fact.setRecIndex(recIndex);

        LegalFactsResponse response = new LegalFactsResponse();
        response.setLegalFacts(List.of(fact));

        when(timelineClient.getLegalFacts(iun, recIndex)).thenReturn(response);

        List<LegalFactsIdIntWithRecIndex> result = timelineServiceHttp.getLegalFacts(iun, recIndex);

        assertEquals(1, result.size());
        assertEquals("key1", result.getFirst().getKey());
        assertEquals(recIndex, result.getFirst().getRecIndex());
    }

    @Test
    void getLegalFacts_nullResponse_returnsEmptyList() {
        String iun = "IUN";
        Integer recIndex = 1;

        when(timelineClient.getLegalFacts(iun, recIndex)).thenReturn(null);

        List<LegalFactsIdIntWithRecIndex> result = timelineServiceHttp.getLegalFacts(iun, recIndex);

        assertTrue(result.isEmpty());
    }
}
