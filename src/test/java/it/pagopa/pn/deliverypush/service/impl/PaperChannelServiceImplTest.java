package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;

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

    @Mock
    private PnDeliveryPushConfigs cfg;
    @Mock
    private AttachmentUtils attachmentUtils;

    @BeforeEach
    void setup() {
        paperChannelService = new PaperChannelServiceImpl(
                paperChannelUtils,
                paperChannelSendClient,
                notificationUtils,
                aarUtils,
                timelineUtils,
                mvpParameterConsumer,
                analogWorkflowUtils, auditLogService,
                cfg,
                attachmentUtils);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetter() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterWithAarAndDocument() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(cfg.getSendSimpleRegisteredLetterAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS.name());
        Mockito.when(attachmentUtils.getNotificationAttachments(notificationInt)).thenReturn(List.of("http://document"));

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).getNotificationAttachments(any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetterWithAarDocumentAndPayments() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(cfg.getSendSimpleRegisteredLetterAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.name());

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));
        Mockito.when(attachmentUtils.getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean() )).thenReturn(List.of("http://document", "http://payment"));

        // WHEN
        paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notificationInt, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithAarAndDocument() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(cfg.getSendAnalogNotificationAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS.name());
        Mockito.when(attachmentUtils.getNotificationAttachments(notificationInt)).thenReturn(List.of("http://document"));

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).getNotificationAttachments(any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithAarDocumentAndPayments() {
        //GIVEN
        NotificationInt notificationInt = newNotificationWithPayments("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(cfg.getSendAnalogNotificationAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.name());
        Mockito.when(attachmentUtils.getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean() )).thenReturn(List.of("http://document", "http://payment"));


        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(any());
        Mockito.verify(attachmentUtils).getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean());
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
    void prepareAnalogNotification() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotification_nodiscovered() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogFeedbackDetailsInt.builder()
                        .build())
                .build();


        TimelineElementInternal timelineElementInternalPrevious = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder()
                        .physicalAddress(PhysicalAddressInt.builder()
                                .address("via esempio")
                                .build())
                        .build())
                .build();

        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_1");
        Mockito.when(paperChannelUtils.buildSendAnalogFeedbackEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_related");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.any(), Mockito.eq("timeline_id_related"))).thenReturn(timelineElementInternal);

        Mockito.when(paperChannelUtils.buildSendAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_previous_send");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.eq("timeline_id_previous_send"))).thenReturn(timelineElementInternalPrevious);


        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithDiscovered() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogFeedbackDetailsInt.builder()
                        .newAddress(PhysicalAddressInt.builder()
                                .address("via casa")
                                .foreignState("italia")
                                .fullname("mario rossi")
                                .build())
                        .build())
                .build();

        TimelineElementInternal timelineElementInternalPrevious = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder()
                        .physicalAddress(PhysicalAddressInt.builder()
                                .address("via esempio")
                                .build())
                        .build())
                .build();

        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_1");
        Mockito.when(paperChannelUtils.buildSendAnalogFeedbackEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_related");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.any(), Mockito.eq("timeline_id_related"))).thenReturn(timelineElementInternal);

        Mockito.when(paperChannelUtils.buildSendAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_previous_send");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.eq("timeline_id_previous_send"))).thenReturn(timelineElementInternalPrevious);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);


        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }



    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithDiscovered_nofullname() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogFeedbackDetailsInt.builder()
                        .newAddress(PhysicalAddressInt.builder()
                                .address("via casa")
                                .foreignState("italia")
                                .build())
                        .build())
                .build();


        TimelineElementInternal timelineElementInternalPrevious = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder()
                        .physicalAddress(PhysicalAddressInt.builder()
                                .address("via esempio")
                                .build())
                        .build())
                .build();

        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_1");
        Mockito.when(paperChannelUtils.buildSendAnalogFeedbackEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_related");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.any(), Mockito.eq("timeline_id_related"))).thenReturn(timelineElementInternal);

        Mockito.when(paperChannelUtils.buildSendAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_previous_send");
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.eq("timeline_id_previous_send"))).thenReturn(timelineElementInternalPrevious);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
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

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(Mockito.any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(cfg.getSendSimpleRegisteredLetterAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.name());
        List<String> attachments = new ArrayList<>(2);
        attachments.add("attachment1");
        attachments.add("attachment2");
        Mockito.when(attachmentUtils.getNotificationAttachmentsAndPayments(Mockito.any(), any(), anyInt(), anyBoolean())).thenReturn(attachments);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient).send(any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(any());
        Mockito.verify(attachmentUtils).getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean());
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
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(cfg.getSendAnalogNotificationAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.name());
        List<String> attachments = new ArrayList<>(2);
        attachments.add("attachment1");
        attachments.add("attachment2");

        Mockito.when(attachmentUtils.getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean())).thenReturn(attachments);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

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

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationAlreadyPaid() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendAnalogNotificationCancelled() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(paperChannelSendClient, Mockito.never()).send(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgNone() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(cfg.getSendSimpleRegisteredLetterAttachments()).thenReturn("none");

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", Collections.emptyList());

        // THEN
        Mockito.verify(attachmentUtils, Mockito.never()).getNotificationAttachments(any());
        Mockito.verify(attachmentUtils, Mockito.never()).getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(cfg.getSendSimpleRegisteredLetterAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.name());

        List<String> attachments = new ArrayList<>(2);
        attachments.add("attachment1");
        attachments.add("attachment2");
        attachments.add("attachment3");
        Mockito.when(attachmentUtils.getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean())).thenReturn(attachments);

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(attachmentUtils).getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetterAndAnalogNotification_CaseSimpleRegisteredLetter() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotificationWithPayments("taxid");

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(notificationUtils.getRecipientFromIndex(any(), anyInt())).thenReturn(notificationInt.getRecipients().get(0));

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        List<String> attachments = new ArrayList<>(2);
        attachments.add("attachment1");
        attachments.add("attachment2");
        attachments.add("attachment3");
        Mockito.when(attachmentUtils.getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean())).thenReturn(attachments);

        Mockito.when(cfg.getSendSimpleRegisteredLetterAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS.name());

        // WHEN
        paperChannelService.sendSimpleRegisteredLetter(notificationInt, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(attachmentUtils).getNotificationAttachmentsAndPayments(any(), any(), anyInt(), anyBoolean());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(cfg.getSendAnalogNotificationAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS.name());

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(attachmentUtils).getNotificationAttachments(any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void sendNotificationAttachments_CfgSimpleRegisteredLetterAndAnalogNotification_CaseAnalogNotification() {
        //GIVEN
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().address("via casa").fullname("full name").build();

        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelSendClient.send(any(PaperChannelSendRequest.class))).thenReturn(new SendResponse());

        Mockito.when(cfg.getSendAnalogNotificationAttachments()).thenReturn(SendAttachmentMode.AAR_DOCUMENTS.name());

        // WHEN
        paperChannelService.sendAnalogNotification(notificationInt, 0, 0, "req123", physicalAddressInt, "NR_SR", List.of("replacedF24Url"));

        // THEN
        Mockito.verify(attachmentUtils).getNotificationAttachments(any());
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
                                        NotificationPaymentInfoIntV2.builder()
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