package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.exceptions.PnPaymentUpdateRetryException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationPaymentException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypush.action.it.utils.TestUtils.verifyPaymentInfo;
import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
class PaymentValidatorTest {
    @Mock
    private PaymentValidator paymentValidator;
    @Mock
    private NotificationProcessCostService notificationProcessCostService;

    @BeforeEach
    public void setup() {
        paymentValidator = new PaymentValidator(notificationProcessCostService);
    }

    @Test
    void validatePaymentsNotDeliveryMode() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification().toBuilder()
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .build();
        Instant startWorkflow = Instant.now();
        
        //WHEN
        Assertions.assertDoesNotThrow( () -> paymentValidator.validatePayments(notification, startWorkflow));
        
        //THEN
        Mockito.verify(notificationProcessCostService, never()).setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        );
    }

    @Test
    void validatePaymentsNotAsync() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification().toBuilder()
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .build();
        Instant startWorkflow = Instant.now();

        //WHEN
        Assertions.assertDoesNotThrow( () -> paymentValidator.validatePayments(notification, startWorkflow));

        //THEN
        Mockito.verify(notificationProcessCostService, never()).setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        );
    }

    @Test
    void validatePaymentsWithoutPaFee() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification().toBuilder()
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .pagoPaIntMode(PagoPaIntMode.ASYNC)
                .paFee(null)
                .build();
        Instant startWorkflow = Instant.now();

        //WHEN

        Assertions.assertThrows(PnValidationPaymentException.class,
                () -> paymentValidator.validatePayments(notification, startWorkflow));

        //THEN
        Mockito.verify(notificationProcessCostService, never()).setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        );
    }

    @Test
    void validatePaymentsApplyCostFalse() {
        //GIVEN
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(false)
                                        .build())
                                .build()
                ))
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        Instant startWorkflow = Instant.now();

        final int notificationBaseCost = notification.getPaFee() + 100;
        Mockito.when(notificationProcessCostService.getNotificationBaseCost(notification.getPaFee())).thenReturn(notificationBaseCost);

        //WHEN
        paymentValidator.validatePayments(notification, startWorkflow);

        //THEN
        Mockito.verify(notificationProcessCostService, never()).setNotificationStepCost(
                Mockito.eq(notificationBaseCost),
                Mockito.eq(notification.getIun()),
                Mockito.any(),
                Mockito.eq(notification.getSentAt()),
                Mockito.eq(startWorkflow),
                Mockito.eq(UpdateCostPhaseInt.VALIDATION)
        );
    }
    
    @Test
    void validatePaymentsKO() {
        //GIVEN
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
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        Instant startWorkflow = Instant.now();

        final int notificationBaseCost = notification.getPaFee() + 100;
        Mockito.when(notificationProcessCostService.getNotificationBaseCost(notification.getPaFee())).thenReturn(notificationBaseCost);

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
        Assertions.assertThrows(PnValidationPaymentException.class,
                () -> paymentValidator.validatePayments(notification, startWorkflow));

        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(notificationBaseCost),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(notification.getSentAt()),
                Mockito.eq(startWorkflow),
                Mockito.eq(UpdateCostPhaseInt.VALIDATION)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();

        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    void validatePaymentsRetry() {
        //GIVEN
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build())
                                .build()
                ))
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        Instant startWorkflow = Instant.now();

        final int notificationBaseCost = notification.getPaFee() + 100;
        Mockito.when(notificationProcessCostService.getNotificationBaseCost(notification.getPaFee())).thenReturn(notificationBaseCost);

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
                () -> paymentValidator.validatePayments(notification, startWorkflow));

        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(notificationBaseCost),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(notification.getSentAt()),
                Mockito.eq(startWorkflow),
                Mockito.eq(UpdateCostPhaseInt.VALIDATION)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    void validatePaymentsOK() {
        //GIVEN
        final String creditorTaxId = "cred";
        final String noticeCode = "notice";
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .creditorTaxId(creditorTaxId)
                                        .noticeCode(noticeCode)
                                        .applyCost(true)
                                        .build())
                                .build()
                ))
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        Instant startWorkflow = Instant.now();

        final int notificationBaseCost = notification.getPaFee() + 100;
        Mockito.when(notificationProcessCostService.getNotificationBaseCost(notification.getPaFee())).thenReturn(notificationBaseCost);

        final int recIndex = 0;
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .updateResults(Collections.singletonList(UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.OK)
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
        paymentValidator.validatePayments(notification, startWorkflow);

        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(notificationBaseCost),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(notification.getSentAt()),
                Mockito.eq(startWorkflow),
                Mockito.eq(UpdateCostPhaseInt.VALIDATION)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    void validateMultiPaymentOk() {
        //GIVEN
        List<NotificationPaymentInfoInt> paymentInfoList = new ArrayList<>();
        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(PagoPaInt.builder()
                        .creditorTaxId("cred1")
                        .noticeCode("notice1")
                        .applyCost(true)
                        .build())
                .build());
        
        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(PagoPaInt.builder()
                        .creditorTaxId("cred2")
                        .noticeCode("notice2")
                        .applyCost(true)
                        .build())
                .build());
        
        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(PagoPaInt.builder()
                        .creditorTaxId("cred3")
                        .noticeCode("notice3")
                        .applyCost(false)
                        .build())
                .build());

        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(null)
                .f24(F24Int.builder().build())
                .build());

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(paymentInfoList)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        Instant startWorkflow = Instant.now();

        final int notificationBaseCost = notification.getPaFee() + 100;
        Mockito.when(notificationProcessCostService.getNotificationBaseCost(notification.getPaFee())).thenReturn(notificationBaseCost);

        final int recIndex = 0;
        
        List<UpdateNotificationCostResultInt> updateNotificationCostResultList = new ArrayList<>();
        updateNotificationCostResultList.add(
                UpdateNotificationCostResultInt.builder()
                .result(UpdateNotificationCostResultInt.ResultEnum.OK)
                .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                        .creditorTaxId(paymentInfoList.get(0).getPagoPA().getCreditorTaxId())
                        .noticeCode(paymentInfoList.get(0).getPagoPA().getNoticeCode())
                        .recIndex(recIndex)
                        .build())
                .build());
        updateNotificationCostResultList.add(
                UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.OK)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(paymentInfoList.get(1).getPagoPA().getCreditorTaxId())
                                .noticeCode(paymentInfoList.get(1).getPagoPA().getNoticeCode())
                                .recIndex(recIndex)
                                .build())
                        .build());
        updateNotificationCostResultList.add(
                UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.OK)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(paymentInfoList.get(2).getPagoPA().getCreditorTaxId())
                                .noticeCode(paymentInfoList.get(2).getPagoPA().getNoticeCode())
                                .recIndex(recIndex)
                                .build())
                        .build());

        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .updateResults(updateNotificationCostResultList).build();

        Mockito.when(notificationProcessCostService.setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        )).thenReturn(Mono.just(response));

        //WHEN
        paymentValidator.validatePayments(notification, startWorkflow);

        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(notificationBaseCost),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(notification.getSentAt()),
                Mockito.eq(startWorkflow),
                Mockito.eq(UpdateCostPhaseInt.VALIDATION)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }

    @Test
    void validateMultiPaymentKO() {
        //GIVEN
        List<NotificationPaymentInfoInt> paymentInfoList = new ArrayList<>();
        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(PagoPaInt.builder()
                        .creditorTaxId("cred1")
                        .noticeCode("notice1")
                        .applyCost(true)
                        .build())
                .build());

        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(PagoPaInt.builder()
                        .creditorTaxId("cred2")
                        .noticeCode("notice2")
                        .applyCost(true)
                        .build())
                .build());

        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(PagoPaInt.builder()
                        .creditorTaxId("cred3")
                        .noticeCode("notice3")
                        .applyCost(false)
                        .build())
                .build());

        paymentInfoList.add(NotificationPaymentInfoInt.builder()
                .pagoPA(null)
                .f24(F24Int.builder().build())
                .build());

        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPayments(paymentInfoList)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .withPagoPaIntMode(PagoPaIntMode.ASYNC)
                .withPaFee(100)
                .withNotificationRecipient(recipient)
                .build();

        Instant startWorkflow = Instant.now();

        final int notificationBaseCost = notification.getPaFee() + 100;
        Mockito.when(notificationProcessCostService.getNotificationBaseCost(notification.getPaFee())).thenReturn(notificationBaseCost);

        final int recIndex = 0;

        List<UpdateNotificationCostResultInt> updateNotificationCostResultList = new ArrayList<>();
        updateNotificationCostResultList.add(
                UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.OK)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(paymentInfoList.get(0).getPagoPA().getCreditorTaxId())
                                .noticeCode(paymentInfoList.get(0).getPagoPA().getNoticeCode())
                                .recIndex(recIndex)
                                .build())
                        .build());
        updateNotificationCostResultList.add(
                UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.OK)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(paymentInfoList.get(1).getPagoPA().getCreditorTaxId())
                                .noticeCode(paymentInfoList.get(1).getPagoPA().getNoticeCode())
                                .recIndex(recIndex)
                                .build())
                        .build());
        updateNotificationCostResultList.add(
                UpdateNotificationCostResultInt.builder()
                        .result(UpdateNotificationCostResultInt.ResultEnum.KO)
                        .paymentsInfoForRecipient(PaymentsInfoForRecipientInt.builder()
                                .creditorTaxId(paymentInfoList.get(2).getPagoPA().getCreditorTaxId())
                                .noticeCode(paymentInfoList.get(2).getPagoPA().getNoticeCode())
                                .recIndex(recIndex)
                                .build())
                        .build());

        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .updateResults(updateNotificationCostResultList).build();

        Mockito.when(notificationProcessCostService.setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        )).thenReturn(Mono.just(response));

        //WHEN
        Assertions.assertThrows(PnValidationPaymentException.class,
                () -> paymentValidator.validatePayments(notification, startWorkflow));

        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(notificationBaseCost),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(notification.getSentAt()),
                Mockito.eq(startWorkflow),
                Mockito.eq(UpdateCostPhaseInt.VALIDATION)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
    }


}