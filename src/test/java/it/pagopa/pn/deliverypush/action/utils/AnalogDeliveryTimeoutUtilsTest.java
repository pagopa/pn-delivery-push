package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalogDeliveryTimeoutUtilsTest {

    @Mock private TimelineService timelineService;
    @Mock private TimelineUtils timelineUtils;
    @Mock private AarUtils aarUtils;
    @Mock private PnDeliveryPushConfigs pnDeliveryPushConfig;
    @Mock private AttachmentUtils attachmentUtils;
    @Mock private NotificationProcessCostService notificationProcessCostService;

    @InjectMocks
    private AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsSendAnalogTimeoutCreationRequestPresent_Found() {
        String iun = "iun";
        int recIndex = 1;
        Integer sentAttemptMade = 2;
        TimelineElementInternal element = mock(TimelineElementInternal.class);

        when(timelineService.getTimelineElement(eq(iun), anyString()))
                .thenReturn(Optional.of(element));

        boolean result = analogDeliveryTimeoutUtils.isSendAnalogTimeoutCreationRequestPresent(iun, recIndex, sentAttemptMade);

        assertTrue(result);
        verify(timelineService).getTimelineElement(eq(iun), anyString());
    }

    @Test
    void testIsSendAnalogTimeoutCreationRequestPresent_NotFound() {
        String iun = "iun";
        int recIndex = 1;
        Integer sentAttemptMade = 2;

        when(timelineService.getTimelineElement(eq(iun), anyString()))
                .thenReturn(Optional.empty());

        boolean result = analogDeliveryTimeoutUtils.isSendAnalogTimeoutCreationRequestPresent(iun, recIndex, sentAttemptMade);

        assertFalse(result);
    }

    @Test
    void testBuildAnalogFailureWorkflowTimeoutElement_NotViewed() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn("iun");
        int recIndex = 1;
        Instant timeoutDate = Instant.now();

        when(pnDeliveryPushConfig.getRetentionAttachmentDaysAfterDeliveryTimeout()).thenReturn(5);
        AarGenerationDetailsInt aarDetails = mock(AarGenerationDetailsInt.class);
        when(aarUtils.getAarGenerationDetails(notification, recIndex)).thenReturn(aarDetails);
        when(aarDetails.getGeneratedAarUrl()).thenReturn("url");
        when(timelineUtils.checkIsNotificationViewed(anyString(), anyInt())).thenReturn(false);
        when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(Flux.empty());
        when(notificationProcessCostService.getSendFeeAsync()).thenReturn(Mono.just(100));
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildAnalogFailureWorkflowTimeout(any(), anyInt(), anyString(), anyInt(), any(), anyBoolean()))
                .thenReturn(timelineElement);

        analogDeliveryTimeoutUtils.buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate);

        verify(attachmentUtils).changeAttachmentsRetention(notification, 5);
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    void testBuildAnalogFailureWorkflowTimeoutElement_Viewed() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn("iun");
        int recIndex = 1;
        Instant timeoutDate = Instant.now();

        when(pnDeliveryPushConfig.getRetentionAttachmentDaysAfterDeliveryTimeout()).thenReturn(5);
        AarGenerationDetailsInt aarDetails = mock(AarGenerationDetailsInt.class);
        when(aarUtils.getAarGenerationDetails(notification, recIndex)).thenReturn(aarDetails);
        when(aarDetails.getGeneratedAarUrl()).thenReturn("url");
        when(timelineUtils.checkIsNotificationViewed(anyString(), anyInt())).thenReturn(true);
        when(notificationProcessCostService.getSendFeeAsync()).thenReturn(Mono.just(100));
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildAnalogFailureWorkflowTimeout(any(), anyInt(), anyString(), anyInt(), any(), anyBoolean()))
                .thenReturn(timelineElement);

        analogDeliveryTimeoutUtils.buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate);

        verify(attachmentUtils, never()).changeAttachmentsRetention(any(), anyInt());
        verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    void testBuildAnalogFailureWorkflowTimeoutElement_Exception() {
        NotificationInt notification = mock(NotificationInt.class);
        when(notification.getIun()).thenReturn("iun");
        int recIndex = 1;
        Instant timeoutDate = Instant.now();

        when(pnDeliveryPushConfig.getRetentionAttachmentDaysAfterDeliveryTimeout()).thenReturn(5);
        AarGenerationDetailsInt aarDetails = mock(AarGenerationDetailsInt.class);
        when(aarUtils.getAarGenerationDetails(notification, recIndex)).thenReturn(aarDetails);
        when(aarDetails.getGeneratedAarUrl()).thenReturn("url");
        when(timelineUtils.checkIsNotificationViewed(anyString(), anyInt())).thenReturn(false);
        when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(Flux.empty());
        when(notificationProcessCostService.getSendFeeAsync()).thenThrow(new RuntimeException("error"));

        assertThrows(PnInternalException.class, () ->
                analogDeliveryTimeoutUtils.buildAnalogFailureWorkflowTimeoutElement(notification, recIndex, timeoutDate)
        );
    }
}