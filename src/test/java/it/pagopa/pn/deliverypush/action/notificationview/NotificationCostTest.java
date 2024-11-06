package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
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
    void getNotificationCostWithRefinement() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineService.getTimelineElementStrongly(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.of(timelineElementInternal));
        
        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Mockito.verify(notificationProcessCostService, Mockito.never()).getSendFeeAsync();
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();
        Assertions.assertTrue(costOpt.isEmpty());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithoutRefinementWithDeceased() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String refinementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        String deceasedId = TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), refinementId)).thenReturn(Optional.empty());
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineService.getTimelineElementStrongly(notification.getIun(), deceasedId)).thenReturn(Optional.of(timelineElementInternal));

        //WHEN
        Mono<Optional<Integer>> monoCostOpt = notificationCost.getNotificationCostForViewed(notification, recIndex);
        //THEN
        Mockito.verify(notificationProcessCostService, Mockito.never()).getSendFeeAsync();
        Assertions.assertNotNull(monoCostOpt);
        Optional<Integer> costOpt = monoCostOpt.block();
        Assertions.assertTrue(costOpt.isEmpty());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithoutRefinementAndWithoutDeceased() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(timelineService.getTimelineElementStrongly(Mockito.anyString(), Mockito.anyString()))
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
}
