package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.AnalogDeliveryTimeoutUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogTimeoutCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

class AnalogWorkflowDeliveryTimeoutHandlerTest {

    @Mock
    TimelineService timelineService;
    @Mock
    TimelineUtils timelineUtils;
    @Mock
    NotificationService notificationService;
    @Mock
    AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;

    @InjectMocks AnalogWorkflowDeliveryTimeoutHandler handler;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handleDeliveryTimeout_firstAttempt_notificationNotViewed() {
        String iun = "testIun";
        int recIndex = 0;
        String timelineId = "timelineId";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn("key");
        when(actionDetails.getTimelineId()).thenReturn(timelineId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendAnalogTimeoutCreationRequestDetailsInt details = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(details.getSentAttemptMade()).thenReturn(0);
        when(details.getRelatedRequestId()).thenReturn("relatedId");
        when(details.getTimeoutDate()).thenReturn(Instant.now());
        when(details.getLegalFactId()).thenReturn("legalFactId");
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(details));

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(timelineService.getTimelineElementDetails(eq(iun), eq("relatedId"), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildSendAnalogTimeout(any(), any(), any(), any())).thenReturn(timelineElement);

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(false);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(analogWorkflowHandler).nextWorkflowStep(notification, recIndex, 1, null);
    }

    @Test
    void handleDeliveryTimeout_firstAttempt_notificationViewed() {
        String iun = "testIun";
        int recIndex = 0;
        String timelineId = "timelineId";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn("key");
        when(actionDetails.getTimelineId()).thenReturn(timelineId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendAnalogTimeoutCreationRequestDetailsInt details = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(details.getSentAttemptMade()).thenReturn(0);
        when(details.getRelatedRequestId()).thenReturn("relatedId");
        when(details.getTimeoutDate()).thenReturn(Instant.now());
        when(details.getLegalFactId()).thenReturn("legalFactId");
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(details));

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildSendAnalogTimeout(any(), any(), any(), any())).thenReturn(timelineElement);

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(true);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(analogWorkflowHandler, never()).nextWorkflowStep(notification, recIndex, 0, null);
        verify(timelineService, never()).addTimelineElement(timelineElement, notification);
    }

    @Test
    void testHandleTimeout_SecondAttempt_notificationNotViewed() {
        String iun = "IUN123";
        int recIndex = 0;
        String timelineId = "timelineId";
        String legalFactId = "legalFactId";
        Instant timeoutDate = Instant.now();

        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn(legalFactId);
        when(actionDetails.getTimelineId()).thenReturn(timelineId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn(iun);

        SendAnalogTimeoutCreationRequestDetailsInt timeoutDetails = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(1);
        when(timeoutDetails.getTimeoutDate()).thenReturn(timeoutDate);

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(timeoutDetails));

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(analogDeliveryTimeoutUtils, times(1))
                .buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate);
        verify(timelineService, times(1))
                .addTimelineElement(any(), eq(notification));
    }

    @Test
    void testHandleTimeout_SecondAttempt_notificationViewed() {
        String iun = "IUN123";
        int recIndex = 0;
        String timelineId = "timelineId";
        String legalFactId = "legalFactId";
        Instant timeoutDate = Instant.now();

        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn(legalFactId);
        when(actionDetails.getTimelineId()).thenReturn(timelineId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn(iun);

        SendAnalogTimeoutCreationRequestDetailsInt timeoutDetails = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(1);
        when(timeoutDetails.getTimeoutDate()).thenReturn(timeoutDate);

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(timeoutDetails));

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(analogDeliveryTimeoutUtils, times(1)).buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate);
        verify(analogWorkflowHandler, never()).nextWorkflowStep(any(), anyInt(), anyInt(), any());
    }

    @Test
    void handleDeliveryTimeout_timelineDetailsNotFound_throwsException() {
        String iun = "testIun";
        int recIndex = 0;
        String timelineId = "timelineId";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn("key");
        when(actionDetails.getTimelineId()).thenReturn(timelineId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenThrow(new RuntimeException("Timeline not found"));

        org.junit.jupiter.api.Assertions.assertThrows(PnInternalException.class, () -> {
            handler.handleDeliveryTimeout(iun, recIndex, actionDetails);
        });
    }

    @Test
    void testBuildSendAnalogTimeoutElement_sendAnalogDetailsNotFound() {
        String iun = "iun";
        int recIndex = 0;
        String timelineId = "timelineId";
        String key = "key";
        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        when(actionDetails.getKey()).thenReturn(key);
        when(actionDetails.getTimelineId()).thenReturn(timelineId);

        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn(iun);

        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.empty());

        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        SendAnalogTimeoutCreationRequestDetailsInt details = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(details.getSentAttemptMade()).thenReturn(0);
        when(details.getTimeoutDate()).thenReturn(Instant.now());
        when(timelineService.getTimelineElementDetails(eq(iun), eq(timelineId), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(details));
        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(true);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(timelineService, never()).addTimelineElement(any(), any());
    }

}