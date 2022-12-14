package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;

class AnalogWorkflowHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaperChannelService paperChannelService;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private PublicRegistryService publicRegistryService;
    @Mock
    private InstantNowSupplier instantNowSupplier;

    private AnalogWorkflowHandler handler;

    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        handler = new AnalogWorkflowHandler(notificationService, paperChannelService,
                completionWorkFlow, instantNowSupplier);
        notificationUtils = new NotificationUtils();
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithPaAddress_0() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);


        //WHEN
        handler.startAnalogWorkflow(notification.getIun(), recIndex);

        //THEN
        Mockito.verify(paperChannelService).prepareAnalogNotification(notification, recIndex,  0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_0() {
        //GIVEN
        NotificationInt notification = getNotificationWithoutPhisicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);


        //WHEN
        handler.startAnalogWorkflow(notification.getIun(), recIndex);

        //THEN
        Mockito.verify(paperChannelService).prepareAnalogNotification(notification, recIndex, 0);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_2() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        //WHEN
        handler.nextWorkflowStep(notification, recIndex, 2);

        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }



    private NotificationInt getNotificationWithPhysicalAddress() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(
                                        PhysicalAddressInt.builder()
                                                .address("test address")
                                                .build()
                                )
                                .payment(null)
                                .build()
                ))
                .build();
    }

    private NotificationInt getNotificationWithoutPhisicalAddress() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startAnalogWorkflow() {
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);


        //WHEN
        handler.startAnalogWorkflow(notification.getIun(), recIndex);

        //THEN
        Mockito.verify(paperChannelService).prepareAnalogNotification(notification, recIndex, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStep_case0() {
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());


        //WHEN
        handler.nextWorkflowStep(notification, recIndex, 0);

        //THEN
        Mockito.verify(paperChannelService).prepareAnalogNotification(notification,  recIndex, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStep_case1() {
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        handler.nextWorkflowStep(notification, recIndex, 1);

        //THEN
        Mockito.verify(paperChannelService).prepareAnalogNotification(notification, recIndex, 1);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStep_case2() {
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        handler.nextWorkflowStep(notification, recIndex, 2);

        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(notification, recIndex, null, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
    }

}