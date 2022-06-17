package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.utils.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ServiceLevelInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
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
    private ExternalChannelService externalChannelService;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private PublicRegistrySendHandler publicRegistrySendHandler;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    private AnalogWorkflowHandler handler;
    
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        handler = new AnalogWorkflowHandler(notificationService, externalChannelService,
                completionWorkFlow, analogWorkflowUtils,
                publicRegistrySendHandler, instantNowSupplier, pnDeliveryPushConfigs);
        notificationUtils= new NotificationUtils();
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithPaAddress_0() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getPhysicalAddress());
        
        //WHEN
        handler.startAnalogWorkflow(notification.getIun(),recIndex);
        
        //THEN
        Mockito.verify(externalChannelService).sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, true, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_0() {
        //GIVEN
        NotificationInt notification = getNotificationWithoutPhisicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
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
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
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
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        
        //WHEN
        handler.nextWorkflowStep(notification, recIndex, 2);
        
        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseWithResponseAddress_0() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .address("test address")
                                .build()
                )
                .build();
        
        //WHEN
        handler.handlePublicRegistryResponse(notification, recIndex, response, 0);
        
        //THEN
        Mockito.verify(externalChannelService).sendAnalogNotification(notification,recipient.getPhysicalAddress(), recIndex, true, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsAvailable_1() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address 2")
                        .build())
                .build();

         SendAnalogFeedbackDetailsInt details =  SendAnalogFeedbackDetailsInt.builder()
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address 2")
                        .build())
                .sentAttemptMade(0)
                .serviceLevel(ServiceLevelInt.SIMPLE_REGISTERED_LETTER)
                .newAddress(PhysicalAddressInt.builder()
                        .address("test address 3")
                        .build())
                .errors(null)
                .build();
        
        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyInt())).thenReturn(details);

        //WHEN
        handler.handlePublicRegistryResponse(notification, recIndex, response, 1);
        
        //THEN
        Mockito.verify(externalChannelService).sendAnalogNotification(notification,details.getNewAddress(), recIndex, false, 1);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsNotAvailable_1() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address 2")
                        .build())
                .build();

         SendAnalogFeedbackDetailsInt details =  SendAnalogFeedbackDetailsInt.builder()
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address 2")
                        .build())
                .sentAttemptMade(0)
                .serviceLevel(ServiceLevelInt.SIMPLE_REGISTERED_LETTER)
                .newAddress(null)
                .errors(null)
                .build();
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());


        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyInt())).thenReturn(details);
        
        //WHEN
        handler.handlePublicRegistryResponse(notification,recIndex,response, 1);
        
        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressNotPresent_1() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .build();

         SendAnalogFeedbackDetailsInt details =  SendAnalogFeedbackDetailsInt.builder()
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address 2")
                        .build())
                .sentAttemptMade(0)
                .serviceLevel(ServiceLevelInt.SIMPLE_REGISTERED_LETTER)
                .newAddress(null)
                .errors(null)
                .build();

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        
        Mockito.when(analogWorkflowUtils.getLastTimelineSentFeedback(Mockito.anyString(), Mockito.anyInt())).thenReturn(details);

        //WHEN
        handler.handlePublicRegistryResponse(notification, recIndex,response, 1);

        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }

    private NotificationInt getNotificationWithPhysicalAddress() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
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
                                .build()
                ))
                .build();
    }

    private NotificationInt getNotificationWithoutPhisicalAddress() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
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

}