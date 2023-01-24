package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

class AnalogWorkflowPaperChannelResponseHandlerTest {

    private AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler;

    @Mock
    private NotificationService notificationService;
    @Mock
    private PaperChannelService paperChannelService;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private PaperChannelUtils paperChannelUtils;
    @Mock
    private AuditLogService auditLogService;

    @BeforeEach
    public void setup() {
        analogWorkflowPaperChannelResponseHandler = new AnalogWorkflowPaperChannelResponseHandler(notificationService,
                paperChannelService,
                completionWorkFlow,
                analogWorkflowUtils,
                pnDeliveryPushConfigs,
                analogWorkflowHandler,
                paperChannelUtils, auditLogService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandler() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode("OK")
                .receiverAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);



        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));

        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("001")
                .statusCode("001")
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();


        PnDeliveryPushConfigs.PaperChannel externalChannel = new PnDeliveryPushConfigs.PaperChannel();
        externalChannel.setAnalogCodesSuccess(List.of("004"));
        externalChannel.setAnalogCodesFail(List.of("005"));
        externalChannel.setAnalogCodesProgress(List.of("001"));

        Mockito.when(pnDeliveryPushConfigs.getPaperChannel()).thenReturn(externalChannel);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler_success() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("004")
                .statusCode("004")
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();


        PnDeliveryPushConfigs.PaperChannel externalChannel = new PnDeliveryPushConfigs.PaperChannel();
        externalChannel.setAnalogCodesSuccess(List.of("004"));
        externalChannel.setAnalogCodesFail(List.of("005"));
        externalChannel.setAnalogCodesProgress(List.of("001"));

        Mockito.when(pnDeliveryPushConfigs.getPaperChannel()).thenReturn(externalChannel);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);


        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler_fail() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("005")
                .statusCode("005")
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();


        PnDeliveryPushConfigs.PaperChannel externalChannel = new PnDeliveryPushConfigs.PaperChannel();
        externalChannel.setAnalogCodesSuccess(List.of("004"));
        externalChannel.setAnalogCodesFail(List.of("005"));
        externalChannel.setAnalogCodesProgress(List.of("001"));

        Mockito.when(pnDeliveryPushConfigs.getPaperChannel()).thenReturn(externalChannel);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);


        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandlerWithAtt() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("001")
                .statusCode("001")
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .attachments(List.of(AttachmentDetailsInt.builder().documentType("A").date(Instant.EPOCH).id("abc").url("http").build()))
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();


        PnDeliveryPushConfigs.PaperChannel externalChannel = new PnDeliveryPushConfigs.PaperChannel();
        externalChannel.setAnalogCodesSuccess(List.of("004"));
        externalChannel.setAnalogCodesFail(List.of("005"));
        externalChannel.setAnalogCodesProgress(List.of("001"));

        Mockito.when(pnDeliveryPushConfigs.getPaperChannel()).thenReturn(externalChannel);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

    }
}