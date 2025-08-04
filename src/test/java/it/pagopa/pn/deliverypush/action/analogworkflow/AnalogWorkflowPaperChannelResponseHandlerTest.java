package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.AnalogDeliveryTimeoutUtils;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnPaperChannelChangedCostException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.FailureDetailCodeEnum;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.deliverypush.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import java.util.Set;

class AnalogWorkflowPaperChannelResponseHandlerTest {

    private AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler;

    @Mock
    private NotificationService notificationService;
    @Mock
    private PaperChannelService paperChannelService;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private PaperChannelUtils paperChannelUtils;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private AnalogDeliveryTimeoutUtils analogDeliveryTimeoutUtils;
    @Mock
    private TimelineService timelineService;

    @BeforeEach
    public void setup() {
        analogWorkflowPaperChannelResponseHandler = new AnalogWorkflowPaperChannelResponseHandler(notificationService,
                paperChannelService,
                analogWorkflowUtils,
                analogWorkflowHandler,
                paperChannelUtils,
                auditLogService,
                schedulerService,
                timelineService,
                analogDeliveryTimeoutUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandler() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.OK.getValue())
                .statusDateTime(Instant.now())
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
    void paperChannelPrepareResponseHandler_Unprocessable() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.OK.getValue())
                .statusDateTime(Instant.now())
                .receiverAddress(PhysicalAddressInt.builder().address("via casa").build())
                .productType("AR")
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelService.sendAnalogNotification(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.isNull(), Mockito.isNull())).thenThrow(new PnPaperChannelChangedCostException());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));

        Mockito.verify( auditLogEvent).generateWarning(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandler_Unprocessable2() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.OK.getValue())
                .statusDateTime(Instant.now())
                .receiverAddress(PhysicalAddressInt.builder().address("via casa").build())
                .productType("AR")
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER)
                .details(SimpleRegisteredLetterDetailsInt.builder().build())
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Mockito.when(paperChannelService.sendSimpleRegisteredLetter(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.isNull(), Mockito.isNull())).thenThrow(new PnPaperChannelChangedCostException());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));

        Mockito.verify( auditLogEvent).generateWarning(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandlerWithF24replacedUrlAnalogNotification() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.OK.getValue())
                .statusDateTime(Instant.now())
                .replacedF24AttachmentUrls(List.of("replacedF24AttachmentUrls"))
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
    void paperChannelPrepareResponseHandlerWithF24replacedUrlSimpleRegisteredLetter() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.OK.getValue())
                .statusDateTime(Instant.now())
                .replacedF24AttachmentUrls(List.of("replacedF24AttachmentUrls"))
                .receiverAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER)
                .details(BaseRegisteredLetterDetailsInt.builder().build())
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
    void paperChannelPrepareResponseHandlerKO_1() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.KO.getValue())
                .statusDateTime(Instant.now())
                .failureDetailCode(FailureDetailCodeEnum.D01.getValue())
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
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString())).thenReturn(auditLogEvent);



        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));


        Mockito.verify(paperChannelUtils, Mockito.times(1)).addPrepareAnalogFailureTimelineElement(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.verify( auditLogEvent).generateWarning(Mockito.anyString());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandlerKO_2() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.KO.getValue())
                .statusDateTime(Instant.now())
                .failureDetailCode(FailureDetailCodeEnum.D02.getValue())
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
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString())).thenReturn(auditLogEvent);



        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));


        Mockito.verify(paperChannelUtils, Mockito.times(1)).addPrepareAnalogFailureTimelineElement(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.verify( auditLogEvent).generateWarning(Mockito.anyString());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandlerKO_3() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.KO.getValue())
                .statusDateTime(Instant.now())
                .failureDetailCode(FailureDetailCodeEnum.D00.getValue())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString())).thenReturn(auditLogEvent);



        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));


        Mockito.verify(paperChannelUtils, Mockito.times(1)).addPrepareAnalogFailureTimelineElement(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.verify( auditLogEvent).generateWarning(Mockito.anyString());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandlerKO_simple_1() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.KO.getValue())
                .statusDateTime(Instant.now())
                .failureDetailCode(FailureDetailCodeEnum.D00.getValue())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER)
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);



        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertThrows(PnInternalException.class, () -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));


        Mockito.verify(paperChannelUtils, Mockito.times(0)).addPrepareAnalogFailureTimelineElement(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.verify( auditLogEvent).generateFailure(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateWarning(Mockito.any());
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.any());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("001")
                .statusCode(StatusCodeEnum.PROGRESS.getValue())
                .statusDateTime(Instant.now())
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();
        TimelineElementInternal sendAnalogDomicileElement = TimelineElementInternal.builder()
                .elementId("SEND_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().physicalAddress(new PhysicalAddressInt()).build())
                .build();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);
        Mockito.when(paperChannelUtils.getSendRequestElementByPrepareRequestId(Mockito.anyString(), Mockito.anyString())).thenReturn(sendAnalogDomicileElement);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler_statusOK_success() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("004")
                .statusCode(StatusCodeEnum.OK.getValue())
                .statusDateTime(Instant.now())
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();
        TimelineElementInternal sendAnalogDomicileElement = TimelineElementInternal.builder()
                .elementId("SEND_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().physicalAddress(new PhysicalAddressInt()).build())
                .build();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);
        Mockito.when(paperChannelUtils.getSendRequestElementByPrepareRequestId(Mockito.anyString(), Mockito.anyString())).thenReturn(sendAnalogDomicileElement);

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
    void paperChannelSendResponseHandler_statusOK_fail() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("004")
                .statusCode(StatusCodeEnum.OK.getValue())
                .statusDateTime(Instant.now())
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();
        TimelineElementInternal sendAnalogDomicileElement = TimelineElementInternal.builder()
                .elementId("SEND_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().physicalAddress(new PhysicalAddressInt()).build())
                .build();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);
        Mockito.when(paperChannelUtils.getSendRequestElementByPrepareRequestId(Mockito.anyString(), Mockito.anyString())).thenReturn(sendAnalogDomicileElement);
        Mockito.when(analogWorkflowUtils.addSuccessAnalogFeedbackToTimeline(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(new PnInternalException("Test exception", "Test cause"));

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);


        // WHEN
        Assertions.assertThrows(PnInternalException.class, () -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

        Mockito.verify( auditLogEvent).generateFailure(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.anyString(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler_statusKO_success() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("005")
                .statusCode(StatusCodeEnum.KO.getValue())
                .statusDateTime(Instant.now())
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        TimelineElementInternal sendAnalogDomicileElement = TimelineElementInternal.builder()
                .elementId("SEND_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().physicalAddress(new PhysicalAddressInt()).build())
                .build();


        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);
        Mockito.when(paperChannelUtils.getSendRequestElementByPrepareRequestId(Mockito.anyString(), Mockito.anyString())).thenReturn(sendAnalogDomicileElement);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_EXECUTE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), (String) Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);


        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

        Mockito.verify( auditLogEvent).generateWarning(Mockito.anyString(), (String) Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler_fails_withoutStatusCode() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("004")
                .statusCode(null)
                .statusDateTime(Instant.now())
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertThrows(PnInternalException.class, () -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandlerWithAtt() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("001")
                .statusCode(StatusCodeEnum.PROGRESS.getValue())
                .statusDateTime(Instant.now())
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .attachments(List.of(AttachmentDetailsInt.builder().documentType("A").date(Instant.EPOCH).id("abc").url("http").build()))
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal prepareAnalogDomicileElement = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        TimelineElementInternal sendAnalogDomicileElement = TimelineElementInternal.builder()
                .elementId("SEND_ANALOG_DOMICILE_001")
                .details(SendAnalogDetailsInt.builder().physicalAddress(new PhysicalAddressInt()).build())
                .build();


        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(prepareAnalogDomicileElement);
        Mockito.when(paperChannelUtils.getSendRequestElementByPrepareRequestId(Mockito.anyString(), Mockito.anyString())).thenReturn(sendAnalogDomicileElement);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler_shouldSkipEvent_whenTimeoutExists() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusCode("PROGRESS")
                .requestId("REQ_01")
                .build();

        int recIndex = 1;
        int sentAttemptMade = 1;

        SendAnalogDetailsInt sendAnalogDetails = SendAnalogDetailsInt.builder()
                .recIndex(recIndex)
                .sentAttemptMade(sentAttemptMade)
                .build();

        SendAnalogTimeoutCreationRequestDetailsInt sendAnalogTimeoutDetails = SendAnalogTimeoutCreationRequestDetailsInt.builder()
                .recIndex(recIndex)
                .sentAttemptMade(sentAttemptMade)
                .build();

        TimelineElementInternal prepareElement = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(sendAnalogDetails)
                .build();

        TimelineElementInternal sendElement = TimelineElementInternal.builder()
                .elementId("SEND_ANALOG_DOMICILE_001")
                .details(sendAnalogDetails)
                .build();

        TimelineElementInternal timeoutElement = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_TIMEOUT_CREATION_REQUEST)
                .details(sendAnalogTimeoutDetails)
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN_01").build();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(prepareElement);
        Mockito.when(paperChannelUtils.getSendRequestElementByPrepareRequestId(Mockito.anyString(), Mockito.anyString())).thenReturn(sendElement);
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.eq(false)))
                .thenReturn(Set.of(timeoutElement));

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(
                        Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_NT_DISCARD_ANALOG_EVENTS),
                        Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString())).thenReturn(auditLogEvent);

        // WHEN
        analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt);

        // THEN
        Mockito.verify(auditLogEvent).generateWarning(Mockito.anyString());
        Mockito.verify(auditLogEvent).log();
        Mockito.verify(analogWorkflowUtils, Mockito.never()).addAnalogProgressAttemptToTimeline(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler_shouldNotSkipEvent_whenTimeoutDoesNotExist() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusCode("PROGRESS")
                .requestId("REQ_01")
                .build();

        int recIndex = 1;
        int sentAttemptMade = 1;

        SendAnalogDetailsInt sendAnalogDetails = SendAnalogDetailsInt.builder()
                .recIndex(recIndex)
                .sentAttemptMade(sentAttemptMade)
                .build();

        TimelineElementInternal prepareElement = TimelineElementInternal.builder()
                .elementId("PREPARE_ANALOG_DOMICILE_001")
                .details(sendAnalogDetails)
                .build();

        TimelineElementInternal sendElement = TimelineElementInternal.builder()
                .elementId("SEND_ANALOG_DOMICILE_001")
                .details(sendAnalogDetails)
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN_01").build();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(prepareElement);
        Mockito.when(paperChannelUtils.getSendRequestElementByPrepareRequestId(Mockito.anyString(), Mockito.anyString())).thenReturn(sendElement);
        Mockito.when(timelineService.getTimeline(Mockito.anyString(), Mockito.eq(false)))
                .thenReturn(Set.of());

        // WHEN
        analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt);

        // THEN
        Mockito.verify(analogWorkflowUtils).addAnalogProgressAttemptToTimeline(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandlerKO_DeliveryTimeout() {
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode(StatusCodeEnum.KO.getValue())
                .statusDateTime(Instant.now())
                .failureDetailCode(FailureDetailCodeEnum.D00.getValue())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal prepareAnalogDomicileElementInternal = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE)
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        PnAuditLogEvent auditLogEvent = mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString())).thenReturn(auditLogEvent);

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(prepareAnalogDomicileElementInternal);
        Mockito.when(analogDeliveryTimeoutUtils.getSendAnalogTimeoutCreationRequestDetails(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(Optional.of(SendAnalogTimeoutCreationRequestDetailsInt.builder().timeoutDate(Instant.now()).build()));

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));
        Assertions.assertDoesNotThrow(() -> analogDeliveryTimeoutUtils.buildAnalogFailureWorkflowTimeoutElement(notificationInt, 1, Instant.now()));


        Mockito.verify(paperChannelUtils, Mockito.times(1)).addPrepareAnalogFailureTimelineElement(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        Mockito.verify( auditLogEvent).generateWarning(Mockito.anyString());
        Mockito.verify(analogDeliveryTimeoutUtils, Mockito.times(1)).getSendAnalogTimeoutCreationRequestDetails(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.verify(analogDeliveryTimeoutUtils, Mockito.times(1)).buildAnalogFailureWorkflowTimeoutElement(Mockito.eq(notificationInt), Mockito.eq(1), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        Mockito.verify( auditLogEvent, Mockito.never()).generateSuccess(Mockito.any());
    }
}