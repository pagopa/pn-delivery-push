package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelAnalogSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ServiceLevelInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private PublicRegistryService publicRegistryService;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private AnalogWorkflowHandler handler;

    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        pnDeliveryPushConfigs = new PnDeliveryPushConfigs();
        handler = new AnalogWorkflowHandler(notificationService, externalChannelService,
                completionWorkFlow, analogWorkflowUtils,
                publicRegistryService, instantNowSupplier, pnDeliveryPushConfigs);
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

        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getPhysicalAddress());

        //WHEN
        handler.startAnalogWorkflow(notification.getIun(), recIndex);

        //THEN
        Mockito.verify(externalChannelService).sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, true, 0);
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

        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getPhysicalAddress());

        //WHEN
        handler.startAnalogWorkflow(notification.getIun(), recIndex);

        //THEN
        Mockito.verify(publicRegistryService).sendRequestForGetPhysicalAddress(notification, recIndex, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStepWithoutPaAddress_1() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN
        handler.nextWorkflowStep(notification, recIndex, 1);

        //THEN
        Mockito.verify(publicRegistryService).sendRequestForGetPhysicalAddress(notification, recIndex, 1);
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

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseWithResponseAddress_0() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

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
        Mockito.verify(externalChannelService).sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, true, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsAvailable_1() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address 2")
                        .build())
                .build();

        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
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
        Mockito.verify(externalChannelService).sendAnalogNotification(notification, details.getNewAddress(), recIndex, false, 1);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressWithSameAddressLastUsedAndNewAddressIsNotAvailable_1() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address 2")
                        .build())
                .build();

        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
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
        handler.handlePublicRegistryResponse(notification, recIndex, response, 1);

        //THEN
        Mockito.verify(completionWorkFlow).completionAnalogWorkflow(eq(notification), eq(recIndex), Mockito.any(), Mockito.any(Instant.class), eq(null), eq(EndWorkflowStatus.FAILURE));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponseAddressNotPresent_1() {
        //GIVEN
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .correlationId("corrId")
                .build();

        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
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
        handler.handlePublicRegistryResponse(notification, recIndex, response, 1);

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

        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(null);

        //WHEN
        handler.startAnalogWorkflow(notification.getIun(), recIndex);

        //THEN
        Mockito.verify(publicRegistryService).sendRequestForGetPhysicalAddress(notification, recIndex, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStep_case0() {
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(analogWorkflowUtils.getPhysicalAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getPhysicalAddress());

        //WHEN
        handler.nextWorkflowStep(notification, recIndex, 0);

        //THEN
        Mockito.verify(externalChannelService).sendAnalogNotification(notification, recipient.getPhysicalAddress(), recIndex, true, 0);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStep_case1() {
        NotificationInt notification = getNotificationWithPhysicalAddress();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        handler.nextWorkflowStep(notification, recIndex, 1);

        //THEN
        Mockito.verify(publicRegistryService).sendRequestForGetPhysicalAddress(notification, recIndex, 1);
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

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseHandler() {

        List<String> analogCodesSuccess = new ArrayList<>();
        analogCodesSuccess.add("OK");
        List<String> analogCodesProgress = new ArrayList<>();
        ExtChannelAnalogSentResponseInt response = buildExtChannelAnalogSentResponseInt();
        SendAnalogDetailsInt sendPaperDetails = buildSendAnalogDetailsInt();
        NotificationInt notification = NotificationTestBuilder.builder().build();

        PnDeliveryPushConfigs.ExternalChannel externalChannel = new PnDeliveryPushConfigs.ExternalChannel();
        externalChannel.setAnalogCodesSuccess(analogCodesSuccess);
        externalChannel.setAnalogCodesProgress(analogCodesProgress);
        pnDeliveryPushConfigs.setExternalChannel(externalChannel);

        List<LegalFactsIdInt> legalFactsListEntryIds = new ArrayList<>();
        legalFactsListEntryIds.add(LegalFactsIdInt.builder()
                .key("http")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build());

        Mockito.when(analogWorkflowUtils.getSendAnalogNotificationDetails(response.getIun(), response.getRequestId())).thenReturn(sendPaperDetails);
        Mockito.when(notificationService.getNotificationByIun(response.getIun())).thenReturn(notification);

        handler.extChannelResponseHandler(response);

       /* Mockito.verify(completionWorkFlow)
                .completionAnalogWorkflow(
                        notification,
                        sendPaperDetails.getRecIndex(),
                        legalFactsListEntryIds,
                        response.getStatusDateTime(),
                        sendPaperDetails.getPhysicalAddress(),
                        EndWorkflowStatus.SUCCESS); */
    }

    private SendAnalogDetailsInt buildSendAnalogDetailsInt() {

        return SendAnalogDetailsInt.builder()
                .recIndex(1)
                .sentAttemptMade(1)
                .investigation(Boolean.FALSE)
                .numberOfPages(1)
                .physicalAddress(PhysicalAddressInt.builder()
                        .address("test address")
                        .build())
                .serviceLevel(ServiceLevelInt.SIMPLE_REGISTERED_LETTER)
                .build();
    }

    private ExtChannelAnalogSentResponseInt buildExtChannelAnalogSentResponseInt() {
        List<AttachmentDetailsInt> attachments = new ArrayList<>();
        attachments.add(AttachmentDetailsInt.builder()
                .id("001")
                .documentType("001")
                .url("http")
                .date(Instant.now())
                .build());

        return ExtChannelAnalogSentResponseInt.builder()
                .requestId("001")
                .iun("001")
                .statusCode("OK")
                .statusDateTime(Instant.now())
                .statusDescription("Active")
                .deliveryFailureCause("Fail")
                .attachments(attachments)
                .discoveredAddress(PhysicalAddressInt.builder()
                        .address("test address")
                        .build())
                .build();
    }
}