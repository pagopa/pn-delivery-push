package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ProbableSchedulingAnalogDateResponse;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TimelineServiceSelectorTest {

    @Test
    void addTimelineElement() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        TimelineElementInternal element = Mockito.mock(TimelineElementInternal.class);
        NotificationInt notification = Mockito.mock(NotificationInt.class);

        Mockito.when(notification.getSentAt()).thenReturn(null);
        Mockito.when(factory.createTimelineService(null)).thenReturn(timelineService);
        Mockito.when(timelineService.addTimelineElement(element, notification)).thenReturn(true);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        boolean result = selector.addTimelineElement(element, notification);

        // Assert
        assertTrue(result);
        Mockito.verify(factory).createTimelineService(null);
        Mockito.verify(timelineService).addTimelineElement(element, notification);
    }

    @Test
    void retrieveAndIncrementCounterForTimelineEvent() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String timelineId = "timeline-123";
        Long expectedCounter = 42L;

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.retrieveAndIncrementCounterForTimelineEvent(timelineId)).thenReturn(expectedCounter);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Long result = selector.retrieveAndIncrementCounterForTimelineEvent(timelineId);

        // Assert
        assertEquals(expectedCounter, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).retrieveAndIncrementCounterForTimelineEvent(timelineId);
    }

    @Test
    void getTimelineElement() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        String timelineId = "timeline-001";
        Optional<TimelineElementInternal> expected = Optional.of(Mockito.mock(TimelineElementInternal.class));

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineElement(iun, timelineId)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Optional<TimelineElementInternal> result = selector.getTimelineElement(iun, timelineId);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineElement(iun, timelineId);
    }

    @Test
    void getTimelineElementStrongly() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        String timelineId = "timeline-strong-001";
        Optional<TimelineElementInternal> expected = Optional.of(Mockito.mock(TimelineElementInternal.class));

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineElementStrongly(iun, timelineId)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Optional<TimelineElementInternal> result = selector.getTimelineElementStrongly(iun, timelineId);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineElementStrongly(iun, timelineId);
    }

    @Test
    void getTimelineElementDetails() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        String timelineId = "timeline-001";
        Class<String> detailsClass = String.class;
        Optional<String> expected = Optional.of("Dettaglio");

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineElementDetails(iun, timelineId, detailsClass)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Optional<String> result = selector.getTimelineElementDetails(iun, timelineId, detailsClass);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineElementDetails(iun, timelineId, detailsClass);
    }

    @Test
    void getTimelineElementDetailForSpecificRecipient() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        int recIndex = 1;
        boolean confidentialInfoRequired = true;
        TimelineElementCategoryInt category = Mockito.mock(TimelineElementCategoryInt.class);
        Class<String> detailsClass = String.class;
        Optional<String> expected = Optional.of("DettaglioSpecifico");

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineElementDetailForSpecificRecipient(
                iun, recIndex, confidentialInfoRequired, category, detailsClass)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Optional<String> result = selector.getTimelineElementDetailForSpecificRecipient(
                iun, recIndex, confidentialInfoRequired, category, detailsClass);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineElementDetailForSpecificRecipient(
                iun, recIndex, confidentialInfoRequired, category, detailsClass);
    }

    @Test
    void getTimelineElementForSpecificRecipient() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        int recIndex = 2;
        TimelineElementCategoryInt category = Mockito.mock(TimelineElementCategoryInt.class);
        Optional<TimelineElementInternal> expected = Optional.of(Mockito.mock(TimelineElementInternal.class));

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, category)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Optional<TimelineElementInternal> result = selector.getTimelineElementForSpecificRecipient(iun, recIndex, category);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineElementForSpecificRecipient(iun, recIndex, category);
    }

    @Test
    void getTimeline() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        boolean confidentialInfoRequired = true;
        Set<TimelineElementInternal> expected = Set.of(Mockito.mock(TimelineElementInternal.class));

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimeline(iun, confidentialInfoRequired)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Set<TimelineElementInternal> result = selector.getTimeline(iun, confidentialInfoRequired);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimeline(iun, confidentialInfoRequired);
    }

    @Test
    void getTimelineStrongly() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        boolean confidentialInfoRequired = true;
        Set<TimelineElementInternal> expected = Set.of(Mockito.mock(TimelineElementInternal.class));

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineStrongly(iun, confidentialInfoRequired)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Set<TimelineElementInternal> result = selector.getTimelineStrongly(iun, confidentialInfoRequired);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineStrongly(iun, confidentialInfoRequired);
    }

    @Test
    void getTimelineByIunTimelineId() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        String timelineId = "timeline-001";
        boolean confidentialInfoRequired = true;
        Set<TimelineElementInternal> expected = Set.of(Mockito.mock(TimelineElementInternal.class));

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Set<TimelineElementInternal> result = selector.getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineByIunTimelineId(iun, timelineId, confidentialInfoRequired);
    }

    @Test
    void getTimelineAndStatusHistory() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        int numberOfRecipients = 3;
        Instant createdAt = Instant.now();
        NotificationHistoryResponse expected = Mockito.mock(NotificationHistoryResponse.class);

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        NotificationHistoryResponse result = selector.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);
    }

    @Test
    void getSchedulingAnalogDate() {
        // Arrange
        TimelineServiceFactory factory = Mockito.mock(TimelineServiceFactory.class);
        TimelineService timelineService = Mockito.mock(TimelineService.class);
        String iun = "iun-test";
        String recipientId = "recipient-001";
        ProbableSchedulingAnalogDateResponse response = Mockito.mock(ProbableSchedulingAnalogDateResponse.class);
        Mono<ProbableSchedulingAnalogDateResponse> expected = Mono.just(response);

        Mockito.when(factory.createTimelineService()).thenReturn(timelineService);
        Mockito.when(timelineService.getSchedulingAnalogDate(iun, recipientId)).thenReturn(expected);

        TimelineServiceSelector selector = new TimelineServiceSelector(factory);

        // Act
        Mono<ProbableSchedulingAnalogDateResponse> result = selector.getSchedulingAnalogDate(iun, recipientId);

        // Assert
        assertEquals(expected, result);
        Mockito.verify(factory).createTimelineService();
        Mockito.verify(timelineService).getSchedulingAnalogDate(iun, recipientId);
    }
}