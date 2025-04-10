package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.api.dto.events.PnF24MetadataValidationEndEventPayload;
import it.pagopa.pn.api.dto.events.PnF24MetadataValidationIssue;
import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.details.NotificationRefusedActionDetails;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.LookupAddressHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.NormalizeAddressHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.config.SendMoreThan20GramsParameterConsumer;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.*;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationValidationActionHandlerTest {
    @Mock
    private AttachmentUtils attachmentUtils;
    @Mock
    private TaxIdPivaValidator taxIdPivaValidator;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationValidationScheduler notificationValidationScheduler;
    @Mock
    private AddressValidator addressValidator;

    @Mock
    private F24Validator f24Validator;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NormalizeAddressHandler normalizeAddressHandler;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private PaymentValidator paymentValidator;

    private NotificationValidationActionHandler handler;
    @Mock
    private PnDeliveryPushConfigs cfg;

    @Mock
    private SafeStorageService safeStorageService;

    @Mock
    private DocumentComposition documentComposition;
    @Mock
    private LookupAddressHandler lookupAddressHandler;

    @Mock
    private NationalRegistriesService nationalRegistriesService;

    @BeforeEach
    public void setup() {
        //quickWorkAroundForPN-9116
        ParameterConsumer parameterConsumerMock = Mockito.mock(ParameterConsumer.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationService = Mockito.mock(NotificationService.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        addressValidator = Mockito.mock(AddressValidator.class);
        lookupAddressHandler = Mockito.mock(LookupAddressHandler.class);
        schedulerService = Mockito.mock(SchedulerService.class);
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
        SendMoreThan20GramsParameterConsumer sendMoreThan20GramsParameterConsumer = new SendMoreThan20GramsParameterConsumer(parameterConsumerMock, cfg);
        handler = new NotificationValidationActionHandler(attachmentUtils, taxIdPivaValidator,
                timelineService, timelineUtils, notificationService,
                notificationValidationScheduler, addressValidator, auditLogService, normalizeAddressHandler,
                schedulerService, cfg, f24Validator, paymentValidator,
                //quickWorkAroundForPN-9116
                sendMoreThan20GramsParameterConsumer,
                safeStorageService, documentComposition, nationalRegistriesService, lookupAddressHandler);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationOK() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);
        // quickWorkAroundForPN-9116
        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());
                
        //WHEN
        handler.validateNotification(notification.getIun(), details);
        
        //THEN
        Mockito.verify(attachmentUtils).validateAttachment(notification);
        Mockito.verify(auditLogEvent, times(3)).generateSuccess();
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));

    }

    // quickWorkAroundForPN-9116
    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKO() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(false);

        NotificationInt notificationBefore = TestUtils.getNotification();
        NotificationInt notification = notificationBefore.toBuilder().documents(List.of(NotificationDocumentInt.builder()
                        .ref( NotificationDocumentInt.Ref.builder()
                                .key("key")
                                .build() )
                .build()))
                .build();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());

        Mockito.when(safeStorageService.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(Mono.just(FileDownloadResponseInt.builder()
                .key("key")
                .checksum("sha256")
                .contentLength(BigDecimal.TEN)
                .download(FileDownloadInfoInt.builder()
                        .url("url")
                        .build())
                .contentType("contentType")
                .build()));
        Mockito.when(safeStorageService.downloadPieceOfContent(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())).thenReturn(downloadPieceOfContent(true));

        Mockito.when(documentComposition.getNumberOfPageFromPdfBytes(Mockito.any())).thenReturn(5);

        PnAuditLogEvent pnAuditLogEventWarn = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(pnAuditLogEventWarn);
        //WHEN
        handler.validateNotification(notification.getIun(), details);

        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));

    }

    // quickWorkAroundForPN-9116
    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationOKWithPaymentNoAttachment() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);
        // quickWorkAroundForPN-9116
        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(false);

        NotificationInt notificationBefore = TestUtils.getNotification();
        NotificationInt notification = notificationBefore.toBuilder()
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref( NotificationDocumentInt.Ref.builder()
                                .key("key")
                                .build() )
                        .build())
                )
                .recipients(List.of(NotificationRecipientInt.builder()
                        .payments(List.of(NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId("creditorTaxId")
                                        .noticeCode("noticeCode")
                                        .build())
                                .build()))
                        .build()))
                .build();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());

        Mockito.when(safeStorageService.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(Mono.just(FileDownloadResponseInt.builder()
                .key("key")
                .checksum("sha256")
                .contentLength(BigDecimal.TEN)
                .download(FileDownloadInfoInt.builder()
                        .url("url")
                        .build())
                .contentType("contentType")
                .build()));
        Mockito.when(safeStorageService.downloadPieceOfContent(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())).thenReturn(downloadPieceOfContent(true));

        Mockito.when(documentComposition.getNumberOfPageFromPdfBytes(Mockito.any())).thenReturn(1);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(attachmentUtils).validateAttachment(notification);
        Mockito.verify(auditLogEvent, times(3)).generateSuccess();
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));

    }

    // quickWorkAroundForPN-9116
    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKOWithPayment() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(false);

        NotificationInt notificationBefore = TestUtils.getNotification();
        NotificationInt notification = notificationBefore.toBuilder()
                .recipients(List.of(NotificationRecipientInt.builder()
                                .payments(List.of(NotificationPaymentInfoInt.builder()
                                                .pagoPA(PagoPaInt.builder()
                                                        .creditorTaxId("creditorTaxId")
                                                        .noticeCode("noticeCode")
                                                        .attachment(NotificationDocumentInt.builder().build())
                                                        .build())
                                        .build()))
                        .build()))
                .build();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        PnAuditLogEvent pnAuditLogEventWarn = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(pnAuditLogEventWarn);
        //WHEN
        handler.validateNotification(notification.getIun(), details);

        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));

    }

    // quickWorkAroundForPN-9116
    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKOWithF24() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(false);

        NotificationInt notificationBefore = TestUtils.getNotification();
        NotificationInt notification = notificationBefore.toBuilder()
                .recipients(List.of(NotificationRecipientInt.builder()
                        .payments(List.of(NotificationPaymentInfoInt.builder()
                                .f24(F24Int.builder().build())
                                .build()))
                        .build()))
                .build();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        PnAuditLogEvent pnAuditLogEventWarn = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(pnAuditLogEventWarn);
        //WHEN
        handler.validateNotification(notification.getIun(), details);

        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));

    }

    public Mono<byte[]> downloadPieceOfContent(boolean isPdf) {
        byte[] res = new byte[8];
        res[0] = 0x25;
        res[1] = 0x50;
        res[2] = 0x44;
        res[3] = 0x46;
        res[4] = 0x2D;
        res[5] = 0x2D;
        res[6] = 0x2D;
        res[7] = 0x2D;

        if (!isPdf)
            res[1] = 0x2D;

        return Mono.just(res);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationOKF24() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);
        // quickWorkAroundForPN-9116
        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        Mockito.when(f24Validator.requestValidateF24(notification)).thenReturn(Mono.empty());

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(attachmentUtils).validateAttachment(notification);
        Mockito.verify(auditLogEvent).generateSuccess();
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));

    }
    
    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKONotFound_isSafeStorageFileNotFoundRetry_true() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(cfg.isSafeStorageFileNotFoundRetry())
                .thenReturn(true);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        PnValidationFileNotFoundException ex = new PnValidationFileNotFoundException("detail", new RuntimeException());
        doThrow(ex).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .startWorkflowTime(Instant.now())
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(any(NotificationInt.class), any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(notificationValidationScheduler).scheduleNotificationValidation(notification, details.getRetryAttempt(), ex, details.getStartWorkflowTime());
        Mockito.verify(auditLogEvent).generateWarning(any(), any());
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKONotFound_isSafeStorageFileNotFoundRetry_false() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);
        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(cfg.isSafeStorageFileNotFoundRetry())
                .thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        PnValidationFileNotFoundException ex = new PnValidationFileNotFoundException("detail", new RuntimeException());
        doThrow(ex).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .startWorkflowTime(Instant.now())
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(any(NotificationInt.class), any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(notification, details.getRetryAttempt(), ex, details.getStartWorkflowTime());
        Mockito.verify(auditLogEvent, Mockito.never()).generateWarning(any(), any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKOFileShaError() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        doThrow(new PnValidationNotMatchingShaException("detail")).when(attachmentUtils).validateAttachment(notification);
        
        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();
        
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.any(Instant.class),
                Mockito.eq(ActionType.NOTIFICATION_REFUSED), Mockito.any(NotificationRefusedActionDetails.class));
        Mockito.verify(auditLogEvent).generateWarning(any(), any());
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKOTaxIdNotValid() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        doThrow(new PnValidationTaxIdNotValidException("detail")).when(taxIdPivaValidator).validateTaxIdPiva(notification);
        
        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();
        
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.any(Instant.class),
                Mockito.eq(ActionType.NOTIFICATION_REFUSED), Mockito.any(NotificationRefusedActionDetails.class));
        Mockito.verify(auditLogEvent).generateWarning(any(), any());
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationTaxIdSkipped_withoutPhysicalAddressLookUp() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(false);
        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotificationV2();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(any(NotificationInt.class), any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), any(), any(), any()))
                .thenReturn(auditLogEvent);

        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(taxIdPivaValidator, Mockito.never()).validateTaxIdPiva(notification);
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationTaxIdSkipped_withPhysicalAddressLookUp() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(false);
        Mockito.when(cfg.isSendMoreThan20GramsDefaultValue())
                .thenReturn(true);
        UsedServicesInt usedServices = UsedServicesInt.builder()
                .physicalAddressLookUp(true)
                .build();

        NotificationInt notification = TestUtils.getNotificationV2(usedServices);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(any(NotificationInt.class), any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), any(), any(), any()))
                .thenReturn(auditLogEvent);

        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(taxIdPivaValidator, Mockito.never()).validateTaxIdPiva(notification);
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationErrorCheckRetry() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        //Simulazione runtimeException generica (servizio non risponde ecc)
        RuntimeException ex = new RuntimeException();
        doThrow(ex).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .startWorkflowTime(Instant.now())
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(any(NotificationInt.class), any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(notificationValidationScheduler).scheduleNotificationValidation(notification, details.getRetryAttempt(),ex, details.getStartWorkflowTime());
        Mockito.verify(auditLogEvent).generateWarning(any(), any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleValidateF24Response_withoutPhysicalAddressLookUp() {
        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        when(timelineUtils.buildValidateF24RequestTimelineElement(any()))
                .thenReturn(TimelineElementInternal.builder().build());
        when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());

        PnF24MetadataValidationEndEventPayload pnF24MetadataValidationEndEventPayload = PnF24MetadataValidationEndEventPayload.builder()
                .setId(notification.getIun())
                .status("ok")
                .errors(Collections.emptyList())
                .build();
        //WHEN
        Assertions.assertDoesNotThrow(() -> handler.handleValidateF24Response(pnF24MetadataValidationEndEventPayload));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleValidateF24Response_withPhysicalAddressLookUp() {
        UsedServicesInt usedServices = UsedServicesInt.builder()
                .physicalAddressLookUp(true)
                .build();
        NotificationInt notification = TestUtils.getNotificationV2WithF24(usedServices);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        when(timelineUtils.buildValidateF24RequestTimelineElement(any()))
                .thenReturn(TimelineElementInternal.builder().build());
        Mockito.doNothing().when(nationalRegistriesService).sendRequestForGetMultiplePhysicalAddress(notification);

        PnF24MetadataValidationEndEventPayload pnF24MetadataValidationEndEventPayload = PnF24MetadataValidationEndEventPayload.builder()
                .setId(notification.getIun())
                .status("ok")
                .errors(Collections.emptyList())
                .build();
        //WHEN
        Assertions.assertDoesNotThrow(() -> handler.handleValidateF24Response(pnF24MetadataValidationEndEventPayload));
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleValidateF24ResponseError() {
        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        PnF24MetadataValidationIssue validationIssue = PnF24MetadataValidationIssue.builder()
                .detail("error detail")
                .build();

        PnF24MetadataValidationEndEventPayload pnF24MetadataValidationEndEventPayload = PnF24MetadataValidationEndEventPayload.builder()
                .setId(notification.getIun())
                .status("ko")
                .errors(List.of(validationIssue))
                .build();

        //WHEN
        Assertions.assertDoesNotThrow(() -> handler.handleValidateF24Response(pnF24MetadataValidationEndEventPayload));
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationPaymentRetry() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //Simulazione errore che necessita retry
        PnPaymentUpdateRetryException ex = new PnPaymentUpdateRetryException("error");
        doThrow(ex).when(paymentValidator).validatePayments(Mockito.any(NotificationInt.class), Mockito.any(Instant.class));

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .startWorkflowTime(Instant.now())
                .build();
        
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(notificationValidationScheduler).scheduleNotificationValidation(notification, details.getRetryAttempt(),ex, details.getStartWorkflowTime());
        Mockito.verify(auditLogEvent).generateWarning(any(), any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationPaymentRetryKO() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        //Simulazione validazione con errore
        PnValidationPaymentException ex = new PnValidationPaymentException("error");
        doThrow(ex).when(paymentValidator).validatePayments(Mockito.any(NotificationInt.class), Mockito.any(Instant.class));

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .startWorkflowTime(Instant.now())
                .build();
        
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.any(Instant.class),
                Mockito.eq(ActionType.NOTIFICATION_REFUSED), Mockito.any(NotificationRefusedActionDetails.class));
        Mockito.verify(auditLogEvent).generateWarning(any(), any());
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), any(), Mockito.any(Instant.class));
    }

    @Test
    void handleValidateNationalRegistriesResponse_success() {
        String correlationId = "correlationId";
        String iun = "iun";
        List<NationalRegistriesResponse> responses = List.of(new NationalRegistriesResponse());
        NotificationInt notification = Mockito.mock(NotificationInt.class);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.anyString()))
                .thenReturn(iun);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(Mockito.any(NotificationInt.class))).thenReturn(Mono.empty());

        handler.handleValidateNationalRegistriesResponse(correlationId, responses);

        Mockito.verify(lookupAddressHandler).validateAddresses(responses);
        Mockito.verify(lookupAddressHandler).saveAddresses(responses, notification);
        Mockito.verify(auditLogEvent).generateSuccess();
        Mockito.verify(addressValidator).requestValidateAndNormalizeAddresses(notification);
    }

    @Test
    void handleValidateNationalRegistriesResponse_lookupAddressNotFound() {
        String correlationId = "correlationId";
        String iun = "iun";
        List<NationalRegistriesResponse> responses = List.of(new NationalRegistriesResponse());
        NotificationInt notification = Mockito.mock(NotificationInt.class);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);

        List<String> details = new ArrayList<>();
        details.add("error detail");

        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.anyString()))
                .thenReturn(iun);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), any())).thenReturn(auditLogEvent);
        doThrow(new PnLookupAddressNotFoundException(details)).when(lookupAddressHandler).validateAddresses(responses);

        handler.handleValidateNationalRegistriesResponse(correlationId, responses);

        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(lookupAddressHandler, Mockito.never()).saveAddresses(responses, notification);
        Mockito.verify(lookupAddressHandler).validateAddresses(responses);
        Mockito.verify(auditLogEvent).generateWarning(any(), any());

    }

    @Test
    void handleValidateNationalRegistriesResponse_cancellationRequested() {
        // Arrange
        String correlationId = "correlationId";
        String iun = "iun";
        List<NationalRegistriesResponse> responses = List.of(new NationalRegistriesResponse());

        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.anyString()))
                .thenReturn(iun);
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString()))
                .thenReturn(true);

        // Act
        handler.handleValidateNationalRegistriesResponse(correlationId, responses);

        // Assert
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested(iun);
        Mockito.verify(notificationService, Mockito.never()).getNotificationByIun(Mockito.anyString());
        Mockito.verify(lookupAddressHandler, Mockito.never()).validateAddresses(Mockito.anyList());
        Mockito.verify(lookupAddressHandler, Mockito.never()).saveAddresses(Mockito.anyList(), Mockito.any(NotificationInt.class));
        Mockito.verify(auditLogService, Mockito.never()).buildAuditLogEvent(Mockito.anyString(), Mockito.any(PnAuditLogEventType.class), Mockito.anyString(), any());
    }

}