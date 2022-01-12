package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
    private ExternalChannelUtils externalChannelUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private CompletionWorkFlowHandler completionWorkflow;
    @Mock
    private TimelineService timelineService;
    @Mock
    private PublicRegistryUtils publicRegistryUtils;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private InstantNowSupplier instantNowSupplier;

    private DigitalWorkFlowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DigitalWorkFlowHandler(externalChannelUtils, notificationService,
                schedulerService, digitalWorkFlowUtils, completionWorkflow, timelineService, publicRegistryUtils, timelineUtils, instantNowSupplier);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_General() {

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));

        handler.nextWorkFlowAction("iun", "text_rec");

        Mockito.verify(publicRegistryUtils).sendRequestForGetDigitalAddress(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithAddress() {

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(AttemptAddressInfo.builder()
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

        handler.nextWorkFlowAction("iun", "text_rec");

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(timelineUtils).buildAvailabilitySourceTimelineElement(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelUtils).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithoutAddress() {

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(AttemptAddressInfo.builder()
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

        handler.nextWorkFlowAction("iun", "text_rec");

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(timelineUtils).buildAvailabilitySourceTimelineElement(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertFalse(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General() {

        Instant lastAttemptDate = Instant.now();

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());


        handler.nextWorkFlowAction("iun", "text_rec");

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyString(),
                schedulingDateCaptor.capture(), Mockito.any(ActionType.class));


        Instant schedulingDateOk = lastAttemptDate.plus(DigitalWorkFlowHandler.SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME, ChronoUnit.DAYS);
        Assertions.assertEquals(schedulingDateOk.truncatedTo(ChronoUnit.MINUTES), schedulingDateCaptor.getValue().truncatedTo(ChronoUnit.MINUTES));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General_Not_Schedule() {

        Instant lastAttemptDate = Instant.now().minus(DigitalWorkFlowHandler.SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME + 1, ChronoUnit.DAYS);

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());


        handler.nextWorkFlowAction("iun", "text_rec");

        Mockito.verify(publicRegistryUtils).sendRequestForGetDigitalAddress(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_NotGeneral() {

        Instant lastAttemptDate = Instant.now().minus(DigitalWorkFlowHandler.SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME + 1, ChronoUnit.DAYS);

        DigitalAddressSource addressSource = DigitalAddressSource.PLATFORM;

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
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


        handler.nextWorkFlowAction("iun", "text_rec");

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<DigitalAddressSource> addressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);

        Mockito.verify(timelineUtils).buildAvailabilitySourceTimelineElement(Mockito.anyString(), Mockito.anyString(),
                addressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(addressSource, addressSourceCaptor.getValue());
        Assertions.assertTrue(isAvailableCaptor.getValue());

        Mockito.verify(externalChannelUtils).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

    }

    /*
    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_NotGeneral_NotAddress() {

        Instant lastAttemptDate = Instant.now().minus(DigitalWorkFlowHandler.SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME + 1, ChronoUnit.DAYS);
        DigitalAddressSource addressSource = DigitalAddressSource.PLATFORM;

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build())
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(2)
                        .lastAttemptDate(lastAttemptDate)
                        .build());
        Notification notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.any(Notification.class)))
                .thenReturn(null);

        handler.nextWorkFlowAction("iun", "text_rec");

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<DigitalAddressSource> addressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<EndWorkflowStatus> endWorkflowStatusCaptor = ArgumentCaptor.forClass(EndWorkflowStatus.class);

        Mockito.verify(timelineUtils).buildAvailabilitySourceTimelineElement(Mockito.anyString(), Mockito.anyString(),
                addressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(addressSource, addressSourceCaptor.getValue());

        Mockito.verify(completionWorkFlow).completionDigitalWorkflow(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(Instant.class), Mockito.any(), endWorkflowStatusCaptor.capture());

        Assertions.assertEquals(EndWorkflowStatus.FAILURE, endWorkflowStatusCaptor.getValue());

    }*/

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

        Notification notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));

        handler.handleGeneralAddressResponse(response, "iun", "text_rec", 0);

        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(timelineUtils).buildAvailabilitySourceTimelineElement(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelUtils).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseOKƒ() {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.OK)
                .iun("IUN")
                .taxId("TaxId")
                .notificationDate(Instant.now())
                .digitalUsedAddress(DigitalAddress.builder()
                        .type(DigitalAddressType.PEC)
                        .address("account@dominio.it")
                        .build()).build();

  /*      handler.handleExternalChannelResponse(extChannelResponse);

        ArgumentCaptor<EndWorkflowStatus> endWorkflowStatusCaptor = ArgumentCaptor.forClass(EndWorkflowStatus.class);

        //TODO Verificare perchè non viene richiamato
        Mockito.verify(completionWorkFlow).completionDigitalWorkflow(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(Instant.class), Mockito.any(DigitalAddress.class), endWorkflowStatusCaptor.capture());

        Assertions.assertEquals(EndWorkflowStatus.SUCCESS, endWorkflowStatusCaptor.getValue()); */
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

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(AttemptAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        handler.handleExternalChannelResponse(extChannelResponse);

        Mockito.verify(timelineUtils).buildDigitalFailureAttemptTimelineElement(Mockito.any(ExtChannelResponse.class));
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