package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

class NotificationCostTest {
    @Mock
    private NotificationProcessCostService notificationProcessCostService;
    @Mock
    private TimelineService timelineService;

    private NotificationCost notificationCost;
    private NotificationUtils notificationUtils;
    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();

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
        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.of(timelineElementInternal));
        
        //WHEN
        Integer cost = notificationCost.getNotificationCost(notification, recIndex);
        //THEN
        Mockito.verify(notificationProcessCostService, Mockito.never()).getNotificationProfit(notification, recIndex);
        Assertions.assertNull(cost);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostWithoutRefinement() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
        int expectedCost = 10;
        Mockito.when(notificationProcessCostService.getNotificationProfit(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(expectedCost);

        //WHEN
        Integer cost = notificationCost.getNotificationCost(notification, recIndex);
        //THEN
        Mockito.verify(notificationProcessCostService).getNotificationProfit(notification, recIndex);
        Assertions.assertEquals(expectedCost, cost);
    }
}
