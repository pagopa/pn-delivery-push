package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;

import SendPaperDetails;
import SendPaperFeedbackDetails;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.action2.utils.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.actions.PecFailSendPaperActionHandler;
import it.pagopa.pn.deliverypush.service.NotificationService;
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
    private ExternalChannelSendHandler externalChannelSendHandler;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private PublicRegistrySendHandler publicRegistrySendHandler;
    @Mock
    private InstantNowSupplier instantNowSupplier;

    private AnalogWorkflowHandler handler;
    
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        handler = new AnalogWorkflowHandler(notificationService, externalChannelSendHandler,
                completionWorkFlow, analogWorkflowUtils,
                publicRegistrySendHandler, instantNowSupplier, timelineUtils);
        notificationUtils= new NotificationUtils();
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithPaAddress_0() {
        //GIVEN
        Notification notification = getNotificationWithPhysicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(Notification.class), Mockito.anyInt()))
                .thenReturn(recipient.getPhysicalAddress());
        
        //WHEN
        handler.startAnalogWorkflow(notification.getIun(),recIndex);
        
        //THEN
        Mockito.verify(externalChannelSendHandler).sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, true, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_0() {
        //GIVEN
        Notification notification = getNotificationWithoutPhisicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(Notification.class), Mockito.anyInt()))
                .thenReturn(recipient.getPhysicalAddress());

        //WHEN
        handler.startAnalogWorkflow(notification.getIun(), recIndex);

        //THEN
        Mockito.verify(publicRegistrySendHandler).sendRequestForGetPhysicalAddress(notification, recIndex, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_1() {
        //GIVEN
        Notification notification = getNotificationWithPhysicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        //WHEN
        handler.nextWorkflowStep(notification, recIndex, 1);
        
        //THEN
        Mockito.verify(publicRegistrySendHandler).sendRequestForGetPhysicalAddress(notification, recIndex, 1);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_2() {
        //GIVEN
        Notification notification = getNotificationWithPhysicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        
        //WHEN
        handler.nextWorkflowStep(notification, recIndex, 2);
        
        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseWithResponseAddress_0() {
        //GIVEN
        Notification notification = getNotificationWithPhysicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(
                        PhysicalAddress.builder()
                                .address("test address")
                                .build()
                )
                .build();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        //WHEN
        handler.handlePublicRegistryResponse(notification.getIun(), recIndex, response, 0);
        
        //THEN
        Mockito.verify(externalChannelSendHandler).sendAnalogNotification(notification,recipient.getPhysicalAddress(), recIndex, true, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsAvailable_1() {
        //GIVEN
        Notification notification = getNotificationWithPhysicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(PhysicalAddress.builder()
                        .address("test address 2")
                        .build())
                .build();

        SendPaperFeedbackDetails details = new SendPaperFeedbackDetails(
                SendPaperDetails.builder()
                        .taxId(recipient.getTaxId())
                        .address(PhysicalAddress.builder()
                                .address("test address 2")
                                .build())
                        .sentAttemptMade(0)
                        .serviceLevel(PecFailSendPaperActionHandler.DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL)
                        .build(),
                PhysicalAddress.builder()
                        .address("test address 3")
                        .build(),
                null
        );
        
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyInt())).thenReturn(details);

        //WHEN
        handler.handlePublicRegistryResponse(notification.getIun(), recIndex, response, 1);
        
        //THEN
        Mockito.verify(externalChannelSendHandler).sendAnalogNotification(notification,details.getNewAddress(), recIndex, false, 1);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsNotAvailable_1() {
        //GIVEN
        Notification notification = getNotificationWithPhysicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(PhysicalAddress.builder()
                        .address("test address 2")
                        .build())
                .build();

        SendPaperFeedbackDetails details = new SendPaperFeedbackDetails(
                SendPaperDetails.builder()
                        .taxId(recipient.getTaxId())
                        .address(PhysicalAddress.builder()
                                .address("test address 2")
                                .build())
                        .sentAttemptMade(0)
                        .serviceLevel(PecFailSendPaperActionHandler.DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL)
                        .build(),
                null,
                null
        );
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyInt())).thenReturn(details);
        
        //WHEN
        handler.handlePublicRegistryResponse(notification.getIun(),recIndex,response, 1);
        
        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressNotPresent_1() {
        //GIVEN
        Notification notification = getNotificationWithPhysicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .build();

        SendPaperFeedbackDetails details = new SendPaperFeedbackDetails(
                SendPaperDetails.builder()
                        .taxId(recipient.getTaxId())
                        .address(PhysicalAddress.builder()
                                .address("test address 2")
                                .build())
                        .sentAttemptMade(0)
                        .serviceLevel(PecFailSendPaperActionHandler.DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL)
                        .build(),
                null,
                null
        );
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyInt())).thenReturn(details);

        //WHEN
        handler.handlePublicRegistryResponse(notification.getIun(), recIndex,response, 1);

        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }

    private Notification getNotificationWithPhysicalAddress() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(
                                        PhysicalAddress.builder()
                                                .address("test address")
                                                .build()
                                )
                                .build()
                ))
                .build();
    }

    private Notification getNotificationWithoutPhisicalAddress() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }

}