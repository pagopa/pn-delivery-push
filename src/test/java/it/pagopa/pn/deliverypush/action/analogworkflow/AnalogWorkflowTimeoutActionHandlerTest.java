package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AnalogWorkflowTimeoutActionHandlerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private FeatureEnabledUtils featureEnabledUtils;
    @Mock
    private PaperTrackerService paperTrackerService;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;

    @InjectMocks
    private AnalogWorkflowTimeoutActionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new AnalogWorkflowTimeoutActionHandler(
                notificationService,
                timelineService,
                featureEnabledUtils,
                paperTrackerService,
                saveLegalFactsService,
                timelineUtils,
                documentCreationRequestService
        );
    }

    @Test
    void shouldNotHandleWhenFeatureDisabled() {
        String iun = "IUN1";
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(false);

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", 0, mock(AnalogWorkflowTimeoutDetails.class), Instant.now());

        verifyNoInteractions(timelineService, paperTrackerService, saveLegalFactsService, timelineUtils, documentCreationRequestService);
    }

    @Test
    void shouldNotHandleWhenTimelineElementNotPresent() {
        String iun = "IUN2";
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(true);
        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogDetailsInt.class))).thenReturn(Optional.empty());

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", 0, mock(AnalogWorkflowTimeoutDetails.class), Instant.now());

        verifyNoMoreInteractions(paperTrackerService, saveLegalFactsService, timelineUtils, documentCreationRequestService);
    }

    @Test
    void shouldSkipWhenDematPresent() {
        String iun = "IUN3";
        NotificationInt notification = mock(NotificationInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(true);

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(sendAnalogDetails.getPrepareRequestId()).thenReturn("PREP_REQ_ID");
        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        when(paperTrackerService.isPresentDematForPrepareRequest("PREP_REQ_ID")).thenReturn(true);

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", 0, mock(AnalogWorkflowTimeoutDetails.class), Instant.now());

        verifyNoInteractions(saveLegalFactsService, timelineUtils, documentCreationRequestService);
    }

    @Test
    void shouldHandleAndCreateLegalFactAndTimeline() {
        String iun = "IUN4";
        int recIndex = 0;
        int sentAttemptMade = 2;
        String legalFactId = "LEGAL_FACT_ID";
        String prepareRequestId = "PREP_REQ_ID";
        String relatedRequestId = "REL_REQ_ID";
        Instant timeoutDate = Instant.now();

        NotificationInt notification = mock(NotificationInt.class);
        NotificationRecipientInt recipient = mock(NotificationRecipientInt.class);
        when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        when(notification.getSentAt()).thenReturn(Instant.now());
        when(notification.getRecipients()).thenReturn(Collections.singletonList(recipient));
        when(featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(any())).thenReturn(true);

        SendAnalogDetailsInt sendAnalogDetails = mock(SendAnalogDetailsInt.class);
        when(sendAnalogDetails.getPrepareRequestId()).thenReturn(prepareRequestId);
        when(sendAnalogDetails.getPhysicalAddress()).thenReturn(getPhysicalAddress());
        when(sendAnalogDetails.getRelatedRequestId()).thenReturn(relatedRequestId);
        when(timelineService.getTimelineElementDetails(eq(iun), any(), eq(SendAnalogDetailsInt.class)))
                .thenReturn(Optional.of(sendAnalogDetails));
        when(paperTrackerService.isPresentDematForPrepareRequest(prepareRequestId)).thenReturn(false);

        AnalogWorkflowTimeoutDetails details = mock(AnalogWorkflowTimeoutDetails.class);
        when(details.getSentAttemptMade()).thenReturn(sentAttemptMade);

        when(saveLegalFactsService.sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact(
                eq(notification), eq(recipient), eq(getPhysicalAddress()), eq(String.valueOf(sentAttemptMade)), eq(timeoutDate)))
                .thenReturn(legalFactId);
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        when(timelineUtils.buildSendAnalogTimeoutCreationRequest(
                eq(notification), eq(recIndex), eq(timeoutDate), eq(sentAttemptMade), eq(relatedRequestId), eq(legalFactId)))
                .thenReturn(timelineElement);
        when(timelineElement.getElementId()).thenReturn("TIMELINE_ID");

        handler.handleAnalogWorkflowTimeout(iun, "timelineId", recIndex, details, timeoutDate);

        verify(saveLegalFactsService).sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact(
                eq(notification), eq(recipient), eq(getPhysicalAddress()), eq(String.valueOf(sentAttemptMade)), eq(timeoutDate));
        verify(timelineService, times(1)).addTimelineElement(timelineElement, notification);
        verify(documentCreationRequestService).addDocumentCreationRequest(
                eq(legalFactId), eq(iun), eq(recIndex), eq(DocumentCreationTypeInt.ANALOG_DELIVERY_TIMEOUT), eq("TIMELINE_ID"));
    }

    private PhysicalAddressInt getPhysicalAddress() {
        PhysicalAddressInt address = new PhysicalAddressInt();
        address.setAddress("Via Roma 1");
        address.setAt("Roma");
        address.setZip("00100");
        return address;
    }
}
