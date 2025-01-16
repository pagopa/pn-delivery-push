package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.CompletelyUnreachableCreationRequestDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AnalogFailureDeliveryCreationResponseHandlerTest {
    @Mock
    private CompletelyUnreachableUtils completelyUnreachableUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private RefinementScheduler refinementScheduler;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private AnalogFailureDeliveryCreationResponseHandler handler;

    @BeforeEach
    void setup() {
        handler = new AnalogFailureDeliveryCreationResponseHandler(
                completelyUnreachableUtils,
                notificationService,
                timelineService,
                refinementScheduler,
                pnDeliveryPushConfigs
        );
    }

    @Test
    void handleAnalogFailureDeliveryCreationResponse_success_sentAtAfter() {
        String iun = "testIun";
        int recIndex = 0;
        DocumentCreationResponseActionDetails actionDetails = new DocumentCreationResponseActionDetails("key", DocumentCreationTypeInt.ANALOG_FAILURE_DELIVERY, "timelineId");
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withSentAt(Instant.now().plusSeconds(3600))
                .build();
        Instant featureUnreachableRefinementPostAARStartDate = Instant.now();
        Instant completionWorkflowDate = Instant.now().minusSeconds(3600);

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(pnDeliveryPushConfigs.getFeatureUnreachableRefinementPostAARStartDate()).thenReturn(featureUnreachableRefinementPostAARStartDate);
        CompletelyUnreachableCreationRequestDetails details = new CompletelyUnreachableCreationRequestDetails();
        details.setCompletionWorkflowDate(completionWorkflowDate);
        details.setLegalFactId("legalFactId");
        details.setEndWorkflowStatus(EndWorkflowStatus.SUCCESS);
        Mockito.when(timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), CompletelyUnreachableCreationRequestDetails.class))
                .thenReturn(Optional.of(details));
        TimelineElementInternal analogFailureWorkflowTimelineElement = new TimelineElementInternal();
        analogFailureWorkflowTimelineElement.setTimestamp(Instant.now());
        Mockito.when(timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW))
                .thenReturn(Optional.of(analogFailureWorkflowTimelineElement));

        handler.handleAnalogFailureDeliveryCreationResponse(iun, recIndex, actionDetails);

        Mockito.verify(completelyUnreachableUtils).handleCompletelyUnreachable(notification, recIndex, details.getLegalFactId(), analogFailureWorkflowTimelineElement.getTimestamp());
        Mockito.verify(refinementScheduler).scheduleAnalogRefinement(notification, recIndex, analogFailureWorkflowTimelineElement.getTimestamp(), details.getEndWorkflowStatus());
    }

    @Test
    void handleAnalogFailureDeliveryCreationResponse_success_sentAtBefore() {
        String iun = "testIun";
        int recIndex = 0;
        DocumentCreationResponseActionDetails actionDetails = new DocumentCreationResponseActionDetails("key", DocumentCreationTypeInt.ANALOG_FAILURE_DELIVERY, "timelineId");
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withSentAt(Instant.now().minusSeconds(3600))
                .build();
        Instant featureUnreachableRefinementPostAARStartDate = Instant.now();
        Instant completionWorkflowDate = Instant.now().minusSeconds(3600);

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(pnDeliveryPushConfigs.getFeatureUnreachableRefinementPostAARStartDate()).thenReturn(featureUnreachableRefinementPostAARStartDate);
        CompletelyUnreachableCreationRequestDetails details = new CompletelyUnreachableCreationRequestDetails();
        details.setCompletionWorkflowDate(completionWorkflowDate);
        details.setLegalFactId("legalFactId");
        details.setEndWorkflowStatus(EndWorkflowStatus.SUCCESS);
        Mockito.when(timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), CompletelyUnreachableCreationRequestDetails.class))
                .thenReturn(Optional.of(details));
        TimelineElementInternal analogFailureWorkflowTimelineElement = new TimelineElementInternal();
        analogFailureWorkflowTimelineElement.setTimestamp(Instant.now());
        Mockito.when(timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW))
                .thenReturn(Optional.of(analogFailureWorkflowTimelineElement));

        handler.handleAnalogFailureDeliveryCreationResponse(iun, recIndex, actionDetails);

        Mockito.verify(completelyUnreachableUtils).handleCompletelyUnreachable(notification, recIndex, details.getLegalFactId(), analogFailureWorkflowTimelineElement.getTimestamp());
        Mockito.verify(refinementScheduler).scheduleAnalogRefinement(notification, recIndex, details.getCompletionWorkflowDate(), details.getEndWorkflowStatus());
    }

    @Test
    void handleAnalogFailureDeliveryCreationResponse_noDetails() {
        String iun = "testIun";
        int recIndex = 0;
        DocumentCreationResponseActionDetails actionDetails = new DocumentCreationResponseActionDetails("key", DocumentCreationTypeInt.ANALOG_FAILURE_DELIVERY, "timelineId");
        NotificationInt notification = new NotificationInt();

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), CompletelyUnreachableCreationRequestDetails.class))
                .thenReturn(Optional.empty());

        handler.handleAnalogFailureDeliveryCreationResponse(iun, recIndex, actionDetails);

        Mockito.verify(completelyUnreachableUtils, Mockito.never()).handleCompletelyUnreachable(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.any());
        Mockito.verify(refinementScheduler, Mockito.never()).scheduleAnalogRefinement(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
    }

    @Test
    void handleAnalogFailureDeliveryCreationResponse_noTimelineElement() {
        String iun = "testIun";
        int recIndex = 0;
        DocumentCreationResponseActionDetails actionDetails = new DocumentCreationResponseActionDetails("key", DocumentCreationTypeInt.ANALOG_FAILURE_DELIVERY, "timelineId");
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .build();
        Instant featureUnreachableRefinementPostAARStartDate = Instant.now();
        Instant completionWorkflowDate = Instant.now();

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(pnDeliveryPushConfigs.getFeatureUnreachableRefinementPostAARStartDate()).thenReturn(featureUnreachableRefinementPostAARStartDate);
        CompletelyUnreachableCreationRequestDetails details = new CompletelyUnreachableCreationRequestDetails();
        details.setCompletionWorkflowDate(completionWorkflowDate);
        details.setLegalFactId("legalFactId");
        details.setEndWorkflowStatus(EndWorkflowStatus.SUCCESS);
        Mockito.when(timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), CompletelyUnreachableCreationRequestDetails.class))
                .thenReturn(Optional.of(details));
        Mockito.when(timelineService.getTimelineElementForSpecificRecipient(iun, recIndex, TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(PnInternalException.class, () -> handler.handleAnalogFailureDeliveryCreationResponse(iun, recIndex, actionDetails));

        Mockito.verify(completelyUnreachableUtils, Mockito.never()).handleCompletelyUnreachable(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.any());
        Mockito.verify(refinementScheduler, Mockito.never()).scheduleAnalogRefinement(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
    }
}
