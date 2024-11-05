package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

class AnalogFinalStatusResponseHandlerTest {

    @Mock
    private TimelineService timelineService;

    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;

    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @InjectMocks
    private AnalogFinalStatusResponseHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handleSuccessfulSending_shouldInvokeCompletionAnalogWorkflowWithDeceasedStatus_whenConditionsAreMet() {
        NotificationInt notification = mock(NotificationInt.class);
        SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails = mock(SendAnalogFeedbackDetailsInt.class);
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        when(sendAnalogFeedbackDetails.getDeliveryFailureCause()).thenReturn("M02");
        when(sendAnalogFeedbackDetails.getResponseStatus()).thenReturn(ResponseStatusInt.OK);
        when(notification.getSentAt()).thenReturn(Instant.parse("2023-01-02T00:00:00Z"));
        when(pnDeliveryPushConfigs.getActivationDeceasedWorfklowDate()).thenReturn("2023-01-01T00:00:00Z");
        when(timelineService.getTimelineElementDetails(anyString(), anyString(), eq(SendAnalogFeedbackDetailsInt.class))).thenReturn(Optional.of(sendAnalogFeedbackDetails));

        handler.handleFinalResponse("iun", 1, "analogFeedbackTimelineId");

        verify(completionWorkFlow).completionAnalogWorkflow(notification, 1, sendAnalogFeedbackDetails.getNotificationDate(), sendAnalogFeedbackDetails.getPhysicalAddress(), EndWorkflowStatus.DECEASED);
    }

    @Test
    void handleSuccessfulSending_shouldInvokeCompletionAnalogWorkflowWithSuccessStatus_whenConditionsAreNotMet() {
        NotificationInt notification = mock(NotificationInt.class);
        SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails = mock(SendAnalogFeedbackDetailsInt.class);
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        when(sendAnalogFeedbackDetails.getDeliveryFailureCause()).thenReturn("M01");
        when(sendAnalogFeedbackDetails.getResponseStatus()).thenReturn(ResponseStatusInt.OK);
        when(notification.getSentAt()).thenReturn(Instant.parse("2023-01-02T00:00:00Z"));
        when(pnDeliveryPushConfigs.getActivationDeceasedWorfklowDate()).thenReturn("2023-01-01T00:00:00Z");
        when(timelineService.getTimelineElementDetails(anyString(), anyString(), eq(SendAnalogFeedbackDetailsInt.class))).thenReturn(Optional.of(sendAnalogFeedbackDetails));

        handler.handleFinalResponse("iun", 1, "analogFeedbackTimelineId");

        verify(completionWorkFlow).completionAnalogWorkflow(notification, 1, sendAnalogFeedbackDetails.getNotificationDate(), sendAnalogFeedbackDetails.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);
    }

    @Test
    void handleSuccessfulSending_shouldInvokeCompletionAnalogWorkflowWithSuccessStatus_whenWorkflowDeceasedDateIsAfterNotificationDate() {
        NotificationInt notification = mock(NotificationInt.class);
        SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails = mock(SendAnalogFeedbackDetailsInt.class);
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        when(sendAnalogFeedbackDetails.getDeliveryFailureCause()).thenReturn("M02");
        when(sendAnalogFeedbackDetails.getResponseStatus()).thenReturn(ResponseStatusInt.OK);
        when(notification.getSentAt()).thenReturn(Instant.parse("2022-12-02T00:00:00Z"));
        when(pnDeliveryPushConfigs.getActivationDeceasedWorfklowDate()).thenReturn("2023-01-01T00:00:00Z");
        when(timelineService.getTimelineElementDetails(anyString(), anyString(), eq(SendAnalogFeedbackDetailsInt.class))).thenReturn(Optional.of(sendAnalogFeedbackDetails));

        handler.handleFinalResponse("iun", 1, "analogFeedbackTimelineId");

        verify(completionWorkFlow).completionAnalogWorkflow(notification, 1, sendAnalogFeedbackDetails.getNotificationDate(), sendAnalogFeedbackDetails.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);
    }

    @Test
    void handleSuccessfulSending_shouldInvokenextWorkflowStep() {
        NotificationInt notification = mock(NotificationInt.class);
        SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails = mock(SendAnalogFeedbackDetailsInt.class);
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        when(sendAnalogFeedbackDetails.getDeliveryFailureCause()).thenReturn("M01");
        when(sendAnalogFeedbackDetails.getResponseStatus()).thenReturn(ResponseStatusInt.KO);
        when(notification.getSentAt()).thenReturn(Instant.parse("2023-01-02T00:00:00Z"));
        when(pnDeliveryPushConfigs.getActivationDeceasedWorfklowDate()).thenReturn("2023-01-01T00:00:00Z");
        when(timelineService.getTimelineElementDetails(anyString(), anyString(), eq(SendAnalogFeedbackDetailsInt.class))).thenReturn(Optional.of(sendAnalogFeedbackDetails));

        handler.handleFinalResponse("iun", 1, "analogFeedbackTimelineId");

        verify(analogWorkflowHandler).nextWorkflowStep(notification, 1, sendAnalogFeedbackDetails.getSentAttemptMade() + 1, sendAnalogFeedbackDetails.getNotificationDate());
    }

    @Test
    void handleSuccessfulSending_shouldInvokeHandleError() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(anyString())).thenReturn(notification);
        when(timelineService.getTimelineElementDetails(anyString(), anyString(), eq(SendAnalogFeedbackDetailsInt.class))).thenReturn(Optional.empty());
        Assertions.assertThrows(PnInternalException.class, () -> handler.handleFinalResponse("iun", 1, "analogFeedbackTimelineId"));
    }
}