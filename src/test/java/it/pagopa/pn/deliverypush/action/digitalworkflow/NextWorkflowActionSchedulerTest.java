package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class NextWorkflowActionSchedulerTest {
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;

    private NextWorkflowActionSchedulerImpl nextWorkflowActionScheduler;
    private NotificationUtils notificationUtils;
    
    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        nextWorkflowActionScheduler = new NextWorkflowActionSchedulerImpl(schedulerService, timelineService, timelineUtils);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void scheduleNextWorkflowAction() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineUtils.buildScheduleDigitalWorkflowTimeline(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any())).thenReturn(timelineElementInternal);

        DigitalAddressInfoSentAttempt addressInfoSentAttempt = new DigitalAddressInfoSentAttempt();
        Instant schedulingDate = Instant.now();
        
        //WHEN
        nextWorkflowActionScheduler.scheduleNextWorkflowAction(notification, recIndex, addressInfoSentAttempt, schedulingDate);
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(schedulerService).scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
    }

    private TimelineElementInternal buildTimelineElementInternal() {
        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(LegalFactsIdInt.builder()
                .key("key")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build());

        return TimelineElementInternal.builder()
                .iun("1")
                .elementId("1")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK)
                .legalFactsIds(legalFactsIds)
                .build();
    }

}