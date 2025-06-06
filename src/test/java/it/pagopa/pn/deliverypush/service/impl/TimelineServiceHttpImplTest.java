package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.timelineservice.TimelineServiceClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.TimelineServiceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class TimelineServiceHttpImplTest {

    @Mock
    private TimelineServiceClient timelineServiceClient;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TimelineServiceHttpImpl timelineServiceHttp;

//    @Test
//    void addTimelineElement() {
//        TimelineElementInternal element = getTimelineElementInternal();
//        NotificationInt notification = new NotificationInt();
//        InlineObject inlineObject = getInlineObject();
//
//        Mockito.when(timelineServiceClient.addTimelineElement(inlineObject)).thenReturn(true);
//
//        boolean result = timelineServiceHttp.addTimelineElement(element, notification);
//
//        assertTrue(result);
//    }
//
//    @Test
//    void retrieveAndIncrementCounterForTimelineEvent() {
//        String timelineId = "timeline123";
//        Long expectedCounter = 42L;
//
//        Mockito.when(timelineServiceClient.retrieveAndIncrementCounterForTimelineEvent(timelineId)).thenReturn(expectedCounter);
//
//        Long result = timelineServiceHttp.retrieveAndIncrementCounterForTimelineEvent(timelineId);
//
//        assertEquals(expectedCounter, result);
//    }
//
//    void getTimelineElementReturnsMappedElement() {
//        String iun = "iun123";
//        String timelineId = "timeline123";
//        TimelineElement timelineElement = new TimelineElement();
//        TimelineElementInternal expectedElement = new TimelineElementInternal();
//
//        Mockito.when(timelineServiceClient.getTimelineElement(iun, timelineId, false)).thenReturn(timelineElement);
//        Mockito.when(TimelineServiceMapper.toTimelineElementInternal(timelineElement)).thenReturn(expectedElement);
//
//        Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElement(iun, timelineId);
//
//        assertTrue(result.isPresent());
//        assertEquals(expectedElement, result.get());
//    }
//
//    void getTimelineElementStronglyReturnsMappedElement() {
//        String iun = "iun123";
//        String timelineId = "timeline123";
//        TimelineElement timelineElement = new TimelineElement();
//        TimelineElementInternal expectedElement = new TimelineElementInternal();
//
//        Mockito.when(timelineServiceClient.getTimelineElement(iun, timelineId, true)).thenReturn(timelineElement);
//        Mockito.when(TimelineServiceMapper.toTimelineElementInternal(timelineElement)).thenReturn(expectedElement);
//
//        Optional<TimelineElementInternal> result = timelineServiceHttp.getTimelineElementStrongly(iun, timelineId);
//
//        assertTrue(result.isPresent());
//        assertEquals(expectedElement, result.get());
//    }
//
//    void getTimelineReturnsMappedSetWhenClientReturnsElements() {
//        String iun = "iun123";
//        boolean confidentialInfoRequired = true;
//        TimelineElement timelineElement = new TimelineElement();
//        TimelineElementInternal mappedElement = new TimelineElementInternal();
//
//        Mockito.when(timelineServiceClient.getTimeline(iun, confidentialInfoRequired, false, null))
//                .thenReturn(Collections.singletonList(timelineElement));
//        Mockito.when(TimelineServiceMapper.toTimelineElementInternal(timelineElement)).thenReturn(mappedElement);
//
//        Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);
//
//        assertEquals(1, result.size());
//        assertTrue(result.contains(mappedElement));
//    }
//
//    void getTimelineReturnsEmptySetWhenClientReturnsNull() {
//        String iun = "iun123";
//        boolean confidentialInfoRequired = true;
//
//        Mockito.when(timelineServiceClient.getTimeline(iun, confidentialInfoRequired, false, null)).thenReturn(null);
//
//        Set<TimelineElementInternal> result = timelineServiceHttp.getTimeline(iun, confidentialInfoRequired);
//
//        assertTrue(result.isEmpty());
//    }

//    private TimelineElementInternal getTimelineElementInternal() {
//        TimelineElementInternal element = new TimelineElementInternal();
//        element.setIun("iun123");
//        element.setElementId("element123");
//        element.setTimestamp(Instant.now());
//        element.setPaId("pa123");
//        element.setLegalFactsIds(new ArrayList<>());
//        element.setCategory(TimelineElementCategoryInt.NOTIFICATION_VIEWED);
//        element.setDetails(SendAnalogFeedbackDetailsInt.builder().build());
//        element.setStatusInfo(StatusInfoInternal.builder().actual("actual").build());
//        element.setNotificationSentAt(Instant.now());
//        element.setIngestionTimestamp(Instant.now());
//        element.setEventTimestamp(Instant.now());
//        return element;
//    }

//    private InlineObject getInlineObject() {
//        TimelineElement timelineElement = new TimelineElement()
//                .iun("iun123")
//                .elementId("element123")
//                .timestamp(Instant.now())
//                .paId("pa123")
//                .legalFactsIds(new ArrayList<>())
//                .category(TimelineCategory.NOTIFICATION_VIEWED)
//                .details(new TimelineElementDetails())
//                .statusInfo(new StatusInfo().actual("actual"))
//                .notificationSentAt(Instant.now())
//                .ingestionTimestamp(Instant.now())
//                .eventTimestamp(Instant.now());
//
//        return new InlineObject()
//                .timelineElement(timelineElement)
//                .notificationInfo(new NotificationInfo());
//    }
}
