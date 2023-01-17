package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

class PaperChannelServiceImplTest {
    @Mock
    private PaperChannelUtils paperChannelUtils;
    @Mock
    private PaperChannelSendClient paperChannelSendClient;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private AarUtils aarUtils;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private MVPParameterConsumer mvpParameterConsumer;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private AuditLogService auditLogService;

    private PaperChannelService paperChannelService;

    @BeforeEach
    void setup() {
        paperChannelService = new PaperChannelServiceImpl(
                paperChannelUtils,
                paperChannelSendClient,
                notificationUtils,
                aarUtils,
                timelineUtils,
                mvpParameterConsumer,
                analogWorkflowUtils, auditLogService);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetter() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient,Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotification() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess();
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_FD_SEND), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient).send(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_FD_SEND), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient).send(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR");

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(Mockito.any());
    }

    private NotificationInt newNotification(String TAX_ID) {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId(TAX_ID)
                                .internalId(TAX_ID + "ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

}