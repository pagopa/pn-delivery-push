package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

class SuccessWorkflowHandlerTest {
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private PnAuditLogEvent logEvent;
    
    private NotificationUtils notificationUtils;
    
    private SuccessWorkflowHandler handler;
    
    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        handler = new SuccessWorkflowHandler(
                timelineService,
                timelineUtils);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void handleSuccessWorkflow() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId("legalFactId")
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.SUCCESS)
                .build();
        
        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildSuccessDigitalWorkflowTimelineElement(notification, recIndex, details.getDigitalAddress(), details.getLegalFactId())).thenReturn(timelineElement);
        Mockito.when(logEvent.generateSuccess()).thenReturn(Mockito.mock(PnAuditLogEvent.class));

        //WHEN
        handler.handleSuccessWorkflow(notification, recIndex, logEvent, details );
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(logEvent).generateSuccess();
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void handleSuccessWorkflowError() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId("legalFactId")
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.SUCCESS)
                .build();

        Mockito.when(timelineUtils.buildSuccessDigitalWorkflowTimelineElement(notification, recIndex, details.getDigitalAddress(), details.getLegalFactId())).thenThrow(new RuntimeException());
        Mockito.when(logEvent.generateFailure(Mockito.anyString(), Mockito.any())).thenReturn(Mockito.mock(PnAuditLogEvent.class));

        //WHEN
        
        Assertions.assertThrows(RuntimeException.class, () -> handler.handleSuccessWorkflow(notification, recIndex, logEvent, details ));

        //THEN
        Mockito.verify(logEvent).generateFailure(Mockito.anyString(), Mockito.any());
    }
}