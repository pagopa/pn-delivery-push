package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

public class AnalogWorkflowTimeoutHandlerServiceImplTest {
    private TimelineUtils timelineUtils;
    private NotificationService notificationService;
    private TimelineService timelineService;
    private AnalogWorkflowTimeoutHandlerServiceImpl service;

    @BeforeEach
    void setUp() {
        timelineUtils = mock(TimelineUtils.class);
        notificationService = mock(NotificationService.class);
        timelineService = mock(TimelineService.class);
        service = new AnalogWorkflowTimeoutHandlerServiceImpl(notificationService, timelineService, timelineUtils);
    }

    @Test
    void handleAnalogWorkflowTimeout_shouldAddTimeoutElement_whenFeedbackNotPresent() {
        String iun = "IUN123";
        String timelineId = "TLID456";
        int recIndex = 0;
        int sentAttemptMade = 1;
        Instant notBefore = Instant.now();

        NotificationInt notification = mock(NotificationInt.class);
        TimelineElementInternal sendAnalogElement = mock(TimelineElementInternal.class);
        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        AnalogWorkflowTimeoutDetails timeoutDetails = mock(AnalogWorkflowTimeoutDetails.class);

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(sentAttemptMade);

        // 1st call: for SEND_ANALOG_DOMICILE
        when(timelineService.getTimelineElement(iun, timelineId)).thenReturn(Optional.of(sendAnalogElement));
        // 2nd call: for SEND_ANALOG_FEEDBACK (feedback not present)
        when(timelineService.getTimelineElement(eq(iun), argThat(id -> !timelineId.equals(id))))
                .thenReturn(Optional.empty());

        when(sendAnalogElement.getDetails()).thenReturn(sendAnalogDetails);
        TimelineElementInternal timeoutElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildSendAnalogTimeoutCreationRequest(notification, sendAnalogDetails, timelineId, notBefore)).thenReturn(timeoutElement);

        service.handleAnalogWorkflowTimeout(iun, timelineId, recIndex, timeoutDetails, notBefore);

        verify(timelineService).addTimelineElement(timeoutElement, notification);
    }

    @Test
    void handleAnalogWorkflowTimeout_shouldNotAddTimeoutElement_whenFeedbackPresent() {
        String iun = "IUN123";
        String timelineId = "TLID456";
        int recIndex = 0;
        int sentAttemptMade = 1;
        Instant notBefore = Instant.now();

        NotificationInt notification = mock(NotificationInt.class);
        TimelineElementInternal sendAnalogElement = mock(TimelineElementInternal.class);
        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        AnalogWorkflowTimeoutDetails timeoutDetails = mock(AnalogWorkflowTimeoutDetails.class);
        TimelineElementInternal feedbackElement = mock(TimelineElementInternal.class);
        SendAnalogFeedbackDetailsInt feedbackDetails = mock(SendAnalogFeedbackDetailsInt.class);

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(sentAttemptMade);

        // Mock SEND_ANALOG_DOMICILE element
        when(timelineService.getTimelineElement(iun, timelineId)).thenReturn(Optional.of(sendAnalogElement));
        when(sendAnalogElement.getDetails()).thenReturn(sendAnalogDetails);

        // Mock feedback element in timeline
        when(feedbackElement.getCategory()).thenReturn(it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);
        when(feedbackElement.getDetails()).thenReturn(feedbackDetails);
        when(feedbackDetails.getRecIndex()).thenReturn(recIndex);
        when(feedbackDetails.getSentAttemptMade()).thenReturn(sentAttemptMade);

        when(timelineService.getTimeline(iun, true)).thenReturn(Set.of(feedbackElement));

        service.handleAnalogWorkflowTimeout(iun, timelineId, recIndex, timeoutDetails, notBefore);

        verify(timelineService, never()).addTimelineElement(any(), any());
    }

    @Test
    void handleAnalogWorkflowTimeout_shouldThrowPnInternalException_whenSendAnalogDomicileNotFound() {
        String iun = "IUN123";
        String timelineId = "TLID456";
        int recIndex = 0;
        int sentAttemptMade = 1;
        Instant notBefore = Instant.now();

        AnalogWorkflowTimeoutDetails timeoutDetails = mock(AnalogWorkflowTimeoutDetails.class);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(sentAttemptMade);

        when(timelineService.getTimelineElement(iun, timelineId)).thenReturn(Optional.empty());

        PnInternalException ex = assertThrows(
                PnInternalException.class,
                () -> service.handleAnalogWorkflowTimeout(iun, timelineId, recIndex, timeoutDetails, notBefore)
        );

        Assertions.assertTrue(ex.getProblem().getDetail().contains("SEND_ANALOG_DOMICILE element not found for iun: " + iun + " and timelineId: " + timelineId));
        Assertions.assertTrue(ex.getProblem().getErrors().get(0).getCode().contains("ERROR_CODE_DELIVERYPUSH_TIMELINEELEMENTNOTPRESENT"));
    }
}
