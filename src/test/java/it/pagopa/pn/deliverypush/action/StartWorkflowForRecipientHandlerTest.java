package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

class StartWorkflowForRecipientHandlerTest {
    @Mock
    private CourtesyMessageUtils courtesyMessageUtils;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private AarUtils aarUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PnAuditLogBuilder auditLogBuilder;

    PnAuditLogEvent logEvent;
    
    private StartWorkflowForRecipientHandler handler;

    @ExtendWith(MockitoExtension.class)
    @BeforeEach
    public void setup() {
        handler = new StartWorkflowForRecipientHandler(courtesyMessageUtils, schedulerService,
                aarUtils, notificationService, auditLogBuilder);

        logEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(auditLogBuilder.build()).thenReturn(logEvent);
        Mockito.when(auditLogBuilder.before(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogBuilder);
        Mockito.when(auditLogBuilder.iun(Mockito.anyString())).thenReturn(auditLogBuilder);
        Mockito.when(logEvent.log()).thenReturn(logEvent);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startNotificationWorkflowForRecipient() {
        //GIVEN
        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(logEvent.generateSuccess()).thenReturn(logEvent);

        //WHEN
        handler.startNotificationWorkflowForRecipient(notification.getIun(), 0, "quickAccessLinkTokenTest");
        
        //THEN
        Mockito.verify(logEvent).generateSuccess();

        Mockito.verify(aarUtils).generateAARAndSaveInSafeStorageAndAddTimelineevent(Mockito.any(NotificationInt.class), Mockito.anyInt());
        Mockito.verify(courtesyMessageUtils).checkAddressesAndSendCourtesyMessage(Mockito.any(NotificationInt.class), Mockito.anyInt());
        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyInt(),
                Mockito.any(Instant.class), Mockito.any(ActionType.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startNotificationWorkflowForRecipientAarFail() {
        //GIVEN
        NotificationInt notification = getNotification();

        String iun = notification.getIun();
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        Mockito.when(logEvent.generateFailure(Mockito.any(), Mockito.any())).thenReturn(logEvent);

        doThrow(new PnNotFoundException("Not found","","")).when(aarUtils).generateAARAndSaveInSafeStorageAndAddTimelineevent(Mockito.any(NotificationInt.class), Mockito.anyInt());
        
        //WHEN
        assertThrows(PnNotFoundException.class, () -> {
            handler.startNotificationWorkflowForRecipient(iun, 0, "quickAccessLinkTokenTest");
        });

        //THEN
        Mockito.verify(logEvent).generateFailure(Mockito.any(), Mockito.any());
                
        Mockito.verify(courtesyMessageUtils, Mockito.times(0)).checkAddressesAndSendCourtesyMessage(Mockito.any(NotificationInt.class), Mockito.anyInt());
        Mockito.verify(schedulerService, Mockito.times(0)).scheduleEvent(Mockito.anyString(), Mockito.anyInt(),
                Mockito.any(Instant.class), Mockito.any(ActionType.class));
    }
    
    private NotificationInt getNotification() {
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("taxId")
                .withInternalId("ANON_"+"taxId")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        return NotificationTestBuilder.builder()
                .withIun("iun")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
    }
}