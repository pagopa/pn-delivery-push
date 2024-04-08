package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.ResultFilterInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.ResultFilterEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

class PaperChannelServiceImplTest {
    @Mock
    private PaperChannelUtils paperChannelUtils;
    @Mock
    private PaperChannelSendClient paperChannelSendClient;
    @Mock
    private NotificationUtils notificationUtils;

    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private MVPParameterConsumer mvpParameterConsumer;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private AuditLogService auditLogService;

    private PaperChannelService paperChannelService;


    @Mock
    private AttachmentUtils attachmentUtils;

    @BeforeEach
    void setup() {
        paperChannelService = new PaperChannelServiceImpl(
                paperChannelUtils,
                paperChannelSendClient,
                notificationUtils,
                timelineUtils,
                mvpParameterConsumer,
                analogWorkflowUtils,
                auditLogService,
                attachmentUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterWithAarAndDocument() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),any(),any(),any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterWithAarDocumentAndPayments() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), any(), any(), any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithAarAndDocument() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),any(),any(),any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithAarDocumentAndPayments() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), any(),any(),any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterAlreadyPaid() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient,Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterCancelled() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient,Mockito.never()).prepare(Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationAlreadyviewed() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationAlreadyPaid() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationCancelled() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).prepare(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments("taxid");


        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(Mockito.any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient).send(any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), any(), any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterCancelled() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendSimpleRegisteredLetterAlreadyPaid() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);


        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult);

        // THEN
        Mockito.verify(paperChannelSendClient).send(any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationAlreadyViewed() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationAlreadyPaid() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationCancelled() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult);

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgNone() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR);
        
        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", Collections.emptyList());

        // THEN
        Mockito.verify(attachmentUtils, Mockito.never()).getNotificationAttachments(any(), any());
        Mockito.verify(attachmentUtils, Mockito.never()).getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), any(), any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetterAndAnalogNotification_CaseSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());


        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(), any(), any(), any(), any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult);

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),any(),any(),any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetterAndAnalogNotification_CaseAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(new ArrayList<>()).discardedAttachments(new ArrayList<>()).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(attachmentUtils.retrieveSendAttachmentMode(Mockito.any(), Mockito.any())).thenReturn(SendAttachmentMode.AAR_DOCUMENTS);

        Mockito.when(attachmentUtils.retrieveAttachments(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(List.of("documentTest"));
        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult);

        // THEN
        Mockito.verify(attachmentUtils).retrieveAttachments(any(),any(),any(),any(),any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CategorizedAttachments() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        ResultFilterInt acceptedAttachments = ResultFilterInt.builder().fileKey("filekey").result(ResultFilterEnum.SUCCESS).reasonCode("reasonCode").reasonDescription("reasonDescription").build();

        CategorizedAttachmentsResultInt categorizedAttachmentsResult = CategorizedAttachmentsResultInt.builder().acceptedAttachments(List.of(acceptedAttachments)).discardedAttachments(new ArrayList<>()).build();
        System.out.println(categorizedAttachmentsResult);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"), categorizedAttachmentsResult);

        // THEN
        Mockito.verify(attachmentUtils, Mockito.never()).retrieveAttachments(any(),any(),any(),any(),any());
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

    private NotificationInt newNotificationWithPayments(String TAX_ID) {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder().key("DocumentKey").build())
                        .digests(NotificationDocumentInt.Digests.builder().sha256("DocumentSha256").build())
                        .build()))
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId(TAX_ID)
                                .internalId(TAX_ID + "ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .payments(List.of(
                                        NotificationPaymentInfoInt.builder()
                                                .f24(F24Int.builder()
                                                        .applyCost(true)
                                                        .title("title")
                                                        .metadataAttachment(NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder().key("F24Key").build())
                                                                .digests(NotificationDocumentInt.Digests.builder().sha256("F24Sha256").build())
                                                                .build())
                                                        .build())
                                                .pagoPA(PagoPaInt.builder()
                                                        .applyCost(true)
                                                        .attachment((NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder().key("PagoPaKey").build())
                                                                .digests(NotificationDocumentInt.Digests.builder().sha256("PagoPaSha256").build())
                                                                .build()))
                                                        .creditorTaxId(TAX_ID)
                                                        .noticeCode("noticeCode")
                                                        .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

}