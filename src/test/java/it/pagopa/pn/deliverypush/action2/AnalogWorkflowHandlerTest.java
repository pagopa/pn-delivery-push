package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.action2.utils.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
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

    @BeforeEach
    public void setup() {
        handler = new AnalogWorkflowHandler(notificationService, externalChannelSendHandler,
                completionWorkFlow, analogWorkflowUtils,
                publicRegistrySendHandler, instantNowSupplier);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithPaAddress_0() {
        Notification notification = getNotificationWithPhisicalAddress();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);
        handler.nextWorkflowStep(notification.getIun(), recipient.getTaxId(), 0);
        
        Mockito.verify(externalChannelSendHandler).sendAnalogNotification(notification,recipient.getPhysicalAddress(),recipient, true, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_0() {
        Notification notification = getNotificationWithoutPhisicalAddress();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);
        handler.nextWorkflowStep(notification.getIun(), recipient.getTaxId(), 0);

        Mockito.verify(publicRegistrySendHandler).sendRequestForGetPhysicalAddress(notification.getIun(),recipient.getTaxId(), 0);
        
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_1() {
        Notification notification = getNotificationWithPhisicalAddress();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);
        handler.nextWorkflowStep(notification.getIun(), recipient.getTaxId(), 1);

        Mockito.verify(publicRegistrySendHandler).sendRequestForGetPhysicalAddress(notification.getIun(),recipient.getTaxId(), 1);
        
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_2() {
        Notification notification = getNotificationWithPhisicalAddress();
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);
        handler.nextWorkflowStep(notification.getIun(), recipient.getTaxId(), 2);

        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(recipient.getTaxId()), eq(notification.getIun()), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseWithResponseAddress_0() {
        Notification notification = getNotificationWithPhisicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
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
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);

        handler.handlePublicRegistryResponse(notification.getIun(), recipient.getTaxId(),response, 0);
        Mockito.verify(externalChannelSendHandler).sendAnalogNotification(notification,recipient.getPhysicalAddress(),recipient, true, 0);

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsAvailable_1() {
        Notification notification = getNotificationWithPhisicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
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
                null,
                null
        );
        
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);
        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyString())).thenReturn(details);
        
        handler.handlePublicRegistryResponse(notification.getIun(), recipient.getTaxId(),response, 1);
        
        Mockito.verify(externalChannelSendHandler).sendAnalogNotification(notification,details.getNewAddress(),recipient, false, 1);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsNotAvailable_1() {
        Notification notification = getNotificationWithPhisicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
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
                null,
                null
        );
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);
        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyString())).thenReturn(details);

        handler.handlePublicRegistryResponse(notification.getIun(), recipient.getTaxId(),response, 1);
        
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(recipient.getTaxId()), eq(notification.getIun()), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressNotPresent_1() {
        Notification notification = getNotificationWithPhisicalAddress();
        NotificationRecipient recipient = notification.getRecipients().get(0);
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
                null,
                null
        );
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(recipient);
        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyString())).thenReturn(details);

        handler.handlePublicRegistryResponse(notification.getIun(), recipient.getTaxId(),response, 1);

        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(recipient.getTaxId()), eq(notification.getIun()), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));

    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseHandler() {
    }

    private Notification getNotificationWithPhisicalAddress() {
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