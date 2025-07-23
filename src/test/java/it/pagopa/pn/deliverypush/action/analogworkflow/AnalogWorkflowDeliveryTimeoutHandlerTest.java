package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogTimeoutCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
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
    AarUtils aarUtils;
    @Mock
    PnDeliveryPushConfigs pnDeliveryPushConfig;
    @Mock
    AttachmentUtils attachmentUtils;
    @Mock
    NotificationProcessCostService notificationProcessCostService;
    @Mock
    AnalogWorkflowHandler analogWorkflowHandler;

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
        String iun = "testIun";
        int recIndex = 1;
        Instant timeoutDate = Instant.now();
        String legalFactId = "legalFactId";

        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        AarGenerationDetailsInt aarGenerationDetails = mock(AarGenerationDetailsInt.class);
        when(aarGenerationDetails.getGeneratedAarUrl()).thenReturn("http://aar-url");

        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);

        SendAnalogTimeoutCreationRequestDetailsInt timeoutDetails = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(1);
        when(timeoutDetails.getRelatedRequestId()).thenReturn("relatedRequestId");
        when(timeoutDetails.getTimeoutDate()).thenReturn(timeoutDate);
        when(timeoutDetails.getLegalFactId()).thenReturn(legalFactId);

        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(timeoutDetails));


        when(aarUtils.getAarGenerationDetails(notification, recIndex)).thenReturn(aarGenerationDetails);
        when(timelineUtils.buildSendAnalogTimeout(any(), any(), any(), any())).thenReturn(timelineElement);
        when(timelineUtils.buildAnalogFailureWorkflowTimeout(any(), anyInt(), anyString(), anyInt(), any(), anyBoolean())).thenReturn(timelineElement);
        when(notificationProcessCostService.getSendFeeAsync()).thenReturn(reactor.core.publisher.Mono.just(100));
        when(pnDeliveryPushConfig.getRetentionAttachmentDaysAfterRefinement()).thenReturn(10);
        when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(reactor.core.publisher.Flux.empty());

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(false);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(timelineUtils).buildAnalogFailureWorkflowTimeout(any(), anyInt(), anyString(), anyInt(), any(), anyBoolean());
        verify(timelineService, times(1)).addTimelineElement(any(), eq(notification));
        verify(aarUtils).getAarGenerationDetails(notification, recIndex);
        verify(timelineUtils).buildAnalogFailureWorkflowTimeout(eq(notification), eq(recIndex), eq("http://aar-url"), eq(100), eq(timeoutDate), eq(true));
    }

    @Test
    void testHandleTimeout_SecondAttempt_notificationViewed() {
        String iun = "testIun";
        int recIndex = 1;
        Instant timeoutDate = Instant.now();
        String legalFactId = "legalFactId";

        DocumentCreationResponseActionDetails actionDetails = mock(DocumentCreationResponseActionDetails.class);
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        AarGenerationDetailsInt aarGenerationDetails = mock(AarGenerationDetailsInt.class);
        when(aarGenerationDetails.getGeneratedAarUrl()).thenReturn("http://aar-url");

        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);

        SendAnalogTimeoutCreationRequestDetailsInt timeoutDetails = mock(SendAnalogTimeoutCreationRequestDetailsInt.class);
        when(timeoutDetails.getSentAttemptMade()).thenReturn(1);
        when(timeoutDetails.getRelatedRequestId()).thenReturn("relatedRequestId");
        when(timeoutDetails.getTimeoutDate()).thenReturn(timeoutDate);
        when(timeoutDetails.getLegalFactId()).thenReturn(legalFactId);

        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogTimeoutCreationRequestDetailsInt.class)))
                .thenReturn(Optional.of(timeoutDetails));

        when(aarUtils.getAarGenerationDetails(notification, recIndex)).thenReturn(aarGenerationDetails);
        when(timelineUtils.buildSendAnalogTimeout(any(), any(), any(), any())).thenReturn(timelineElement);
        when(timelineUtils.buildAnalogFailureWorkflowTimeout(any(), anyInt(), anyString(), anyInt(), any(), anyBoolean())).thenReturn(timelineElement);
        when(notificationProcessCostService.getSendFeeAsync()).thenReturn(reactor.core.publisher.Mono.just(100));
        when(pnDeliveryPushConfig.getRetentionAttachmentDaysAfterRefinement()).thenReturn(10);
        when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(reactor.core.publisher.Flux.empty());

        when(timelineUtils.checkIsNotificationViewed(iun, recIndex)).thenReturn(true);

        handler.handleDeliveryTimeout(iun, recIndex, actionDetails);

        verify(timelineUtils).buildAnalogFailureWorkflowTimeout(any(), anyInt(), anyString(), anyInt(), any(), anyBoolean());
        verify(timelineService).addTimelineElement(any(), eq(notification));
        verify(aarUtils).getAarGenerationDetails(notification, recIndex);
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
}