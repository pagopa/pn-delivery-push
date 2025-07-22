package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogFailureWorkflowTimeoutDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogWorfklowRecipientDeceasedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RefinementDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Optional;

class NotificationCostTest {
    @Mock
    private NotificationProcessCostService notificationProcessCostService;
    @Mock
    private TimelineService timelineService;

    private NotificationCost notificationCost;

    @BeforeEach
    public void setup() {
        notificationCost = new NotificationCost(notificationProcessCostService, timelineService);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithRefinementAndCost() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        TimelineElementInternal timelineElementInternal = getRefinementElementInternal(100);
        Mockito.when(timelineService.getTimelineElementStrongly(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.of(timelineElementInternal));
        
        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Mockito.verify(notificationProcessCostService, Mockito.never()).getSendFeeAsync();
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();
        Assertions.assertNotNull(costOpt);
        Assertions.assertTrue(costOpt.isEmpty());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithRefinementNoCostWithDeceasedAndCost() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String refinementId = getRefinementId(notification, recIndex);

        String deceasedId = getDeceasedId(notification, recIndex);

        TimelineElementInternal refinementTimelineElementInternal = getRefinementElementInternal(0);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), refinementId)).thenReturn(Optional.of(refinementTimelineElementInternal));

        TimelineElementInternal timelineElementInternal = getDeceasedElementInternal(100);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), deceasedId)).thenReturn(Optional.of(timelineElementInternal));

        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Mockito.verify(notificationProcessCostService, Mockito.never()).getSendFeeAsync();
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();
        Assertions.assertNotNull(costOpt);
        Assertions.assertTrue(costOpt.isEmpty());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithoutRefinementWithDeceasedAndCost() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String refinementId = getRefinementId(notification, recIndex);

        String deceasedId = getDeceasedId(notification, recIndex);

        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), refinementId)).thenReturn(Optional.empty());
        TimelineElementInternal timelineElementInternal = getDeceasedElementInternal(100);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), deceasedId)).thenReturn(Optional.of(timelineElementInternal));

        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Mockito.verify(notificationProcessCostService, Mockito.never()).getSendFeeAsync();
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();
        Assertions.assertNotNull(costOpt);
        Assertions.assertTrue(costOpt.isEmpty());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithoutRefinementWithDeceasedAndNoCost() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String refinementId = getRefinementId(notification, recIndex);

        String deceasedId = getDeceasedId(notification, recIndex);

        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), refinementId)).thenReturn(Optional.empty());

        TimelineElementInternal timelineElementInternal = getDeceasedElementInternal(0);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), deceasedId)).thenReturn(Optional.of(timelineElementInternal));

        int expectedCost = 0;
        Mockito.when(notificationProcessCostService.getSendFeeAsync()).thenReturn(Mono.just(expectedCost));

        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();

        Assertions.assertNotNull(costOpt);
        Assertions.assertTrue(costOpt.isPresent());
        Assertions.assertNotNull(costOpt.get());
        Assertions.assertEquals(expectedCost, costOpt.get());

        Mockito.verify(notificationProcessCostService).getSendFeeAsync();
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithRefinementNoCostWithDeceasedNoCostWithFailureTimeoutAndCost() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String refinementId = getRefinementId(notification, recIndex);

        String deceasedId = getDeceasedId(notification, recIndex);

        String failureTimeoutId = getFailureTimeoutId(notification, recIndex);

        TimelineElementInternal refinementTimelineElementInternal = getRefinementElementInternal(0);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), refinementId)).thenReturn(Optional.of(refinementTimelineElementInternal));

        TimelineElementInternal deceasedTimelineElementInternal = getDeceasedElementInternal(0);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), deceasedId)).thenReturn(Optional.of(deceasedTimelineElementInternal));

        TimelineElementInternal timeoutFailureTimelineElementInternal = getFailureTimeoutElementInternal(100);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), failureTimeoutId)).thenReturn(Optional.of(timeoutFailureTimelineElementInternal));

        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();
        Assertions.assertNotNull(costOpt);
        Assertions.assertTrue(costOpt.isEmpty());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithoutRefinementWithDeceasedNoCostWithFailureTimeoutAndNoCost() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String refinementId = getRefinementId(notification, recIndex);

        String deceasedId = getDeceasedId(notification, recIndex);

        String failureTimeoutId = getFailureTimeoutId(notification, recIndex);

        TimelineElementInternal refinementTimelineElementInternal = getRefinementElementInternal(0);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), refinementId)).thenReturn(Optional.of(refinementTimelineElementInternal));

        TimelineElementInternal deceasedTimelineElementInternal = getDeceasedElementInternal(0);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), deceasedId)).thenReturn(Optional.of(deceasedTimelineElementInternal));

        TimelineElementInternal timeoutFailureTimelineElementInternal = getFailureTimeoutElementInternal(0);
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), failureTimeoutId)).thenReturn(Optional.of(timeoutFailureTimelineElementInternal));

        int expectedCost = 0;
        Mockito.when(notificationProcessCostService.getSendFeeAsync()).thenReturn(Mono.just(expectedCost));

        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();
        Assertions.assertNotNull(costOpt);
        Assertions.assertTrue(costOpt.isPresent());

        Mockito.verify(notificationProcessCostService).getSendFeeAsync();
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithoutRefinementAndWithoutDeceasedWithoutFailureTimeout() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(timelineService.getTimelineElementStrongly(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());

        int expectedCost = 0;
        Mockito.when(notificationProcessCostService.getSendFeeAsync()).thenReturn(Mono.just(expectedCost));

        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();

        Assertions.assertNotNull(costOpt);
        Assertions.assertTrue(costOpt.isPresent());
        Assertions.assertNotNull(costOpt.get());
        Assertions.assertEquals(expectedCost, costOpt.get());

        Mockito.verify(notificationProcessCostService).getSendFeeAsync();
    }

    private static String getRefinementId(NotificationInt notification, int recIndex) {
        return TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
    }

    private static String getDeceasedId(NotificationInt notification, int recIndex) {
        return TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
    }

    private static String getFailureTimeoutId(NotificationInt notification, int recIndex) {
        return TimelineEventId.ANALOG_FAILURE_WORKFLOW_TIMEOUT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
    }

    private static TimelineElementInternal getRefinementElementInternal(Integer notificationCost) {
        return TimelineElementInternal.builder()
                .details(RefinementDetailsInt.builder()
                        .notificationCost(notificationCost)
                        .build()
                ).build();
    }

    private static TimelineElementInternal getDeceasedElementInternal(Integer notificationCost) {
        return TimelineElementInternal.builder()
                .details(AnalogWorfklowRecipientDeceasedDetailsInt.builder()
                        .notificationCost(notificationCost)
                        .build()
                ).build();
    }

    private static TimelineElementInternal getFailureTimeoutElementInternal(Integer notificationCost) {
        return TimelineElementInternal.builder()
                .details(AnalogFailureWorkflowTimeoutDetailsInt.builder()
                        .notificationCost(notificationCost)
                        .build()
                ).build();
    }

}
