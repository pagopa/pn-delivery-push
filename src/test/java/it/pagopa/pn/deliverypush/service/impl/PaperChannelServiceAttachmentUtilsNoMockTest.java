package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;

class PaperChannelServiceAttachmentUtilsNoMockTest {
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

    private AttachmentUtils attachmentUtils;

    @Mock
    SafeStorageService safeStorageService;
    @Mock
    PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    NotificationProcessCostService notificationProcessCostService;
    @Mock
    PnSendModeUtils pnSendModeUtils;

    @BeforeEach
    void setup() {
        attachmentUtils = new AttachmentUtils(
                safeStorageService,
                pnDeliveryPushConfigs,
                notificationProcessCostService,
                pnSendModeUtils,
                aarUtils,
                notificationUtils
        );

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
    void prepareAnalogNotificationErrorNoConfiguration() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
                .aarWithRadd(true).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        Mockito.when(paperChannelUtils.buildPrepareAnalogDomicileEventId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt())).thenReturn("timeline_id_1");

        Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(null);

        Mockito.when(aarUtils.getAarCreationRequestDetailsInt(Mockito.any(), Mockito.anyInt())).thenReturn(aarCreationRequestDetailsInt);

        // WHEN
        Assertions.assertThrows(PnInternalException.class, () -> paperChannelService.prepareAnalogNotification(notificationInt, 0, 1));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationForSimpleRegisteredLetter() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
                .aarWithRadd(true).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(aarUtils.getAarCreationRequestDetailsInt(Mockito.any(), Mockito.anyInt())).thenReturn(aarCreationRequestDetailsInt);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), any(), any(), any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR)
                .build());

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
    void prepareAnalogNotification() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
                .aarWithRadd(true).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(aarUtils.getAarCreationRequestDetailsInt(Mockito.any(), Mockito.anyInt())).thenReturn(aarCreationRequestDetailsInt);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                .analogSendAttachmentMode(SendAttachmentMode.AAR)
                .build());

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 0);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotification_nodiscovered() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
                .aarWithRadd(true).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(aarUtils.getAarCreationRequestDetailsInt(Mockito.any(), Mockito.anyInt())).thenReturn(aarCreationRequestDetailsInt);

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

        Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                .analogSendAttachmentMode(SendAttachmentMode.AAR)
                .build());

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);


        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithDiscovered() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
                .aarWithRadd(true).build();


        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(aarUtils.getAarCreationRequestDetailsInt(Mockito.any(), Mockito.anyInt())).thenReturn(aarCreationRequestDetailsInt);

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
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                .analogSendAttachmentMode(SendAttachmentMode.AAR)
                .build());

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void prepareAnalogNotificationWithDiscovered_nofullname() {
        //GIVEN
        NotificationInt notificationInt = newNotification("taxid");
        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();

        AarCreationRequestDetailsInt aarCreationRequestDetailsInt = AarCreationRequestDetailsInt.builder()
                .aarWithRadd(true).build();

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationPaid(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(aarUtils.getAarGenerationDetails(Mockito.any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);
        Mockito.when(aarUtils.getAarCreationRequestDetailsInt(Mockito.any(), Mockito.anyInt())).thenReturn(aarCreationRequestDetailsInt);

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

        Mockito.when(pnSendModeUtils.getPnSendMode(Mockito.any())).thenReturn(PnSendMode.builder()
                .analogSendAttachmentMode(SendAttachmentMode.AAR)
                .build());

        // WHEN
        paperChannelService.prepareAnalogNotification(notificationInt, 0, 1);

        // THEN
        Mockito.verify(paperChannelSendClient).prepare(Mockito.any());
        Mockito.verify(auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
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
