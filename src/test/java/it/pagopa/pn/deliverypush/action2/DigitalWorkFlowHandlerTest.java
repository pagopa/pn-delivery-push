package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.PublicRegistryCallDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

class DigitalWorkFlowHandlerTest {
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private ExternalChannelSendHandler externalChannelSendHandler;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private CompletionWorkFlowHandler completionWorkflow;
    @Mock
    private PublicRegistrySendHandler publicRegistrySendHandler;
    @Mock
    private InstantNowSupplier instantNowSupplier;

    private DigitalWorkFlowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DigitalWorkFlowHandler(externalChannelSendHandler, notificationService,
                schedulerService, digitalWorkFlowUtils, completionWorkflow, publicRegistrySendHandler, instantNowSupplier);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_General() {
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(lastAttemptMade.getAddressSource().next())
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));


        handler.nextWorkFlowAction("iun", "text_rec", lastAttemptMade);

        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalAddress(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithAddress() {
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.GENERAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.any(Notification.class)))
                .thenReturn(DigitalAddress.builder()
                        .address("testAddress")
                        .type(DigitalAddressType.PEC)
                        .build());

        handler.nextWorkFlowAction("iun", "text_rec", lastAttemptMade);

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithoutAddress() {
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.GENERAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.any(Notification.class)))
                .thenReturn(null);

        handler.nextWorkFlowAction("iun", "text_rec", lastAttemptMade);

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertFalse(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General() {
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(1)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now();

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());


        handler.nextWorkFlowAction("iun", "text_rec", lastAttemptMade);

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyString(),
                schedulingDateCaptor.capture(), Mockito.any(ActionType.class));


        Instant schedulingDateOk = lastAttemptDate.plus(DigitalWorkFlowHandler.SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME, ChronoUnit.DAYS);
        Assertions.assertEquals(schedulingDateOk.truncatedTo(ChronoUnit.MINUTES), schedulingDateCaptor.getValue().truncatedTo(ChronoUnit.MINUTES));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General_Not_Schedule() {
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now().minus(DigitalWorkFlowHandler.SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME + 1, ChronoUnit.DAYS);

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());


        handler.nextWorkFlowAction("iun", "text_rec", lastAttemptMade);

        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalAddress(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_NotGeneral() {
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now().minus(DigitalWorkFlowHandler.SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME + 1, ChronoUnit.DAYS);

        DigitalAddressSource addressSource = DigitalAddressSource.PLATFORM;

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(addressSource)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());
        Notification notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.any(Notification.class)))
                .thenReturn(DigitalAddress.builder()
                        .address("testAddress")
                        .type(DigitalAddressType.PEC)
                        .build());


        handler.nextWorkFlowAction("iun", "text_rec", lastAttemptMade);

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<DigitalAddressSource> addressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                addressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(addressSource, addressSourceCaptor.getValue());
        Assertions.assertTrue(isAvailableCaptor.getValue());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponse() {

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(DigitalAddress.builder()
                        .type(DigitalAddressType.PEC)
                        .address("account@dominio.it")
                        .build())
                .correlationId("testcorrid")
                .physicalAddress(null)
                .build();

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
                .taxId("testTaxid")
                .sendDate(Instant.now())
                .sentAttemptMade(0)
                .build();

        Notification notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));

        handler.handleGeneralAddressResponse(response, "iun", details);

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseKO() {

        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.KO)
                .iun("IUN")
                .taxId("TaxId")
                .notificationDate(Instant.now())
                .digitalUsedAddress(DigitalAddress.builder()
                        .type(DigitalAddressType.PEC)
                        .address("account@dominio.it")
                        .build()).build();

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        TimelineElement element = TimelineElement.builder()
                .details(SendDigitalDetails.sendBuilder().build())
                .build();

        handler.handleExternalChannelResponse(extChannelResponse, element);

        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(ExtChannelResponse.class));
    }

    private Notification getNotification() {
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
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}