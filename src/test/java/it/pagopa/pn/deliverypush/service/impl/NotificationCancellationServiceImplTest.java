package it.pagopa.pn.deliverypush.service.impl;

import static it.pagopa.pn.deliverypush.action.it.utils.TestUtils.verifyPaymentInfo;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static it.pagopa.pn.deliverypush.service.impl.NotificationCancellationServiceImpl.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaIntMode;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnPaymentUpdateRetryException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

class NotificationCancellationServiceImplTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private PaperNotificationFailedService paperNotificationFailedService;
    @Mock
    private AuthUtils authUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationProcessCostService notificationProcessCostService;

    private NotificationCancellationService notificationCancellationService;
    
    @BeforeEach
    public void init(){
        notificationCancellationService = new NotificationCancellationServiceImpl(notificationService, paperNotificationFailedService, authUtils, timelineService, timelineUtils, auditLogService, notificationProcessCostService);
    }
    
    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcess() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.just(notification));
                
        Mockito.when(authUtils.checkPaIdAndGroup(eq(notification), anyString(), any(CxTypeAuthFleet.class), anyList()))
                .thenReturn(Mono.empty());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildCancelRequestTimelineElement(notification))
                .thenReturn(timelineElementInternal);
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(notification.getIun()))
                .thenReturn(false);
        Mockito.when(timelineUtils.checkIsNotificationRefused(notification.getIun()))
                .thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        
        //WHEN
        StatusDetailInt res = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA, new ArrayList<>())
                .block();
        
        //THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(NOTIFICATION_CANCELLATION_ACCEPTED, res.getCode());
        Assertions.assertEquals("INFO", res.getLevel());
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateSuccess();
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcessAlreadyCancelled() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.just(notification));

        Mockito.when(authUtils.checkPaIdAndGroup(eq(notification), anyString(), any(CxTypeAuthFleet.class), anyList()))
                .thenReturn(Mono.empty());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();

        Mockito.when(timelineUtils.checkIsNotificationRefused(notification.getIun()))
                .thenReturn(false);

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(notification.getIun()))
                .thenReturn(true);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);

        //WHEN
        StatusDetailInt res = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA, new ArrayList<>())
                .block();

        //THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(NOTIFICATION_ALREADY_CANCELLED, res.getCode());
        Assertions.assertEquals("WARN", res.getLevel());
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateWarning(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcessRefused() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.just(notification));

        Mockito.when(authUtils.checkPaIdAndGroup(eq(notification), anyString(), any(CxTypeAuthFleet.class), anyList()))
                .thenReturn(Mono.empty());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();

        Mockito.when(timelineUtils.checkIsNotificationRefused(notification.getIun()))
                .thenReturn(true);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);

        //WHEN
        StatusDetailInt res = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA, new ArrayList<>())
                .block();

        //THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(NOTIFICATION_REFUSED, res.getCode());
        Assertions.assertEquals("WARN", res.getLevel());
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateWarning(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcessError() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.just(notification));

        Mockito.when(authUtils.checkPaIdAndGroup(eq(notification), anyString(), any(CxTypeAuthFleet.class), anyList()))
                .thenReturn(Mono.error(() -> {
                    throw new PnNotFoundException("Not found", "error", ERROR_CODE_DELIVERYPUSH_NOTFOUND);
                }));
        
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN

        Mono<StatusDetailInt> monoResp = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA, new ArrayList<>());
        
        Assertions.assertThrows(PnNotFoundException.class, monoResp::block);
        
        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any());
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }


    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcessErrorNotFound2() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.error(() -> {
                    throw new PnNotFoundException("Not found", "error", ERROR_CODE_DELIVERYPUSH_NOTFOUND);
                }));


        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN

        Mono<StatusDetailInt> monoResp = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA, new ArrayList<>());

        Assertions.assertThrows(PnNotFoundException.class, monoResp::block);

        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any());
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationForPagoPaAsync() {
        //Given
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build()
                                )
                                .build()
                ))
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .build();

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildCancelledTimelineElement(notification)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElement.getTimestamp())).thenReturn(Mono.empty());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        final int recIndex = 0;
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .updateResults(Collections.singletonList(UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.KO)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(creditorTaxId)
                                .noticeCode(noticeCode)
                                .recIndex(recIndex)
                                .build())
                        .build()
                )).build();

        Mockito.when(notificationProcessCostService.setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        )).thenReturn(Mono.just(response));

        //WHEN
        notificationCancellationService.completeCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();

        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(NotificationCancellationServiceImpl.NOTIFICATION_CANCELLED_COST),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.eq(UpdateCostPhaseInt.NOTIFICATION_CANCELLED)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationForPagoPaAsyncError() {
        //Given
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build()
                                )
                                .build()
                ))
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .build();

        final String iun = notification.getIun();
        Mockito.when(notificationService.removeAllNotificationCostsByIun(iun)).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(iun), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);

        final int recIndex = 0;
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .updateResults(Collections.singletonList(UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.RETRY)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(creditorTaxId)
                                .noticeCode(noticeCode)
                                .recIndex(recIndex)
                                .build())
                        .build()
                )).build();

        Mockito.when(notificationProcessCostService.setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        )).thenReturn(Mono.just(response));

        //WHEN
        Assertions.assertThrows(PnPaymentUpdateRetryException.class,
                () -> notificationCancellationService.completeCancellationProcess(iun));

        //THEN
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(iun);
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.eq(notification));
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.any(), Mockito.any());

        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(NotificationCancellationServiceImpl.NOTIFICATION_CANCELLED_COST),
                Mockito.eq(iun),
                paymentForRecipientListCaptor.capture(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.eq(UpdateCostPhaseInt.NOTIFICATION_CANCELLED)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationNotPagoPaAsync() {
        //Given
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build()
                                )
                                .build()
                ))
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.NONE)
                .withPaFee(100)
                .build();

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildCancelledTimelineElement(notification)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElement.getTimestamp())).thenReturn(Mono.empty());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        //WHEN
        notificationCancellationService.completeCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), "iun");
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();

        Mockito.verify(notificationProcessCostService, Mockito.never()).setNotificationStepCost(
                Mockito.eq(NotificationCancellationServiceImpl.NOTIFICATION_CANCELLED_COST),
                Mockito.eq(notification.getIun()),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.eq(UpdateCostPhaseInt.NOTIFICATION_CANCELLED)
        );
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationAlreadyInserted() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        final TimelineElementInternal timelineElementOLD = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now().minusMillis(1000))
                .build();
        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildCancelledTimelineElement(notification)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(timelineService.getTimelineElement(notification.getIun(), timelineElement.getElementId())).thenReturn(Optional.ofNullable(timelineElementOLD));
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElementOLD.getTimestamp())).thenReturn(Mono.empty());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        //WHEN
        notificationCancellationService.completeCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), "iun");
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationException() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenThrow(new NullPointerException());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        String iun = notification.getIun();
        Assert.assertThrows(NullPointerException.class, ()-> notificationCancellationService.completeCancellationProcess(iun));

        //THEN
        Mockito.verify(paperNotificationFailedService, Mockito.never()).deleteNotificationFailed(recipient.getInternalId(), "iun");
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    private NotificationInt getNotification(){
        return NotificationInt.builder()
                .iun("RUYX-MLZA-JUPJ-202308-W-1")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId("Milano1")
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
                                .payments(null)
                                .build()
                ))
                .build();

    }
}