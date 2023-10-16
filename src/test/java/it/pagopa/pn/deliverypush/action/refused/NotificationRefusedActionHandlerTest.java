package it.pagopa.pn.deliverypush.action.refused;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateCostPhaseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResultInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnPaymentUpdateRetryException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypush.action.it.utils.TestUtils.verifyPaymentInfo;
import static org.mockito.Mockito.never;

@ExtendWith(SpringExtension.class)
class NotificationRefusedActionHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private NotificationProcessCostService notificationProcessCostService;

    private NotificationRefusedActionHandler notificationRefusedActionHandler;

    @BeforeEach
    public void setup() {
        notificationRefusedActionHandler = new NotificationRefusedActionHandler(notificationService, timelineUtils, timelineService, notificationProcessCostService);
    }

    @Test
    void notificationRefusedNotDeliveryModeAndNotAsync() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification().toBuilder()
                .notificationFeePolicy(NotificationFeePolicy.FLAT_RATE)
                .pagoPaIntMode(PagoPaIntMode.SYNC)
                .build();
        
        List<NotificationRefusedErrorInt> errors = Collections.singletonList(
                NotificationRefusedErrorInt.builder()
                        .errorCode("errorCode")
                        .detail("errorDetail")
                        .build()
        );
        Instant schedulingTime = Instant.now();

        TimelineElementInternal elementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildRefusedRequestTimelineElement(notification, errors)).thenReturn(elementInternal);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        
        //WHEN
                
        notificationRefusedActionHandler.notificationRefusedHandler(notification.getIun(), errors, schedulingTime);
        //THEN
        Mockito.verify(notificationProcessCostService, never()).setNotificationStepCost(
                Mockito.anyInt(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.any(UpdateCostPhaseInt.class)
        );
        
        Mockito.verify(timelineService).addTimelineElement(elementInternal, notification);
    }

    @Test
    void notificationRefusedSetNotificationCostOK() {
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

        List<NotificationRefusedErrorInt> errors = Collections.singletonList(
                NotificationRefusedErrorInt.builder()
                        .errorCode("errorCode")
                        .detail("errorDetail")
                        .build()
        );
        Instant schedulingTime = Instant.now();

        final int recIndex = 0;
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .iun(notification.getIun())
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

        TimelineElementInternal elementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildRefusedRequestTimelineElement(notification, errors)).thenReturn(elementInternal);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        //WHEN
        notificationRefusedActionHandler.notificationRefusedHandler(notification.getIun(), errors, schedulingTime);
        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(NotificationRefusedActionHandler.NOTIFICATION_REFUSED_COST),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(schedulingTime),
                Mockito.eq(schedulingTime),
                Mockito.eq(UpdateCostPhaseInt.REQUEST_REFUSED)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);
        
        Mockito.verify(timelineService).addTimelineElement(elementInternal, notification);
    }

    @Test
    void notificationRefusedSetNotificationCostKO() {
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

        List<NotificationRefusedErrorInt> errors = Collections.singletonList(
                NotificationRefusedErrorInt.builder()
                        .errorCode("errorCode")
                        .detail("errorDetail")
                        .build()
        );
        Instant schedulingTime = Instant.now();

        final int recIndex = 0;
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .iun(notification.getIun())
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

        TimelineElementInternal elementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildRefusedRequestTimelineElement(notification, errors)).thenReturn(elementInternal);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        //WHEN
        notificationRefusedActionHandler.notificationRefusedHandler(notification.getIun(), errors, schedulingTime);
        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(NotificationRefusedActionHandler.NOTIFICATION_REFUSED_COST),
                Mockito.eq(notification.getIun()),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(schedulingTime),
                Mockito.eq(schedulingTime),
                Mockito.eq(UpdateCostPhaseInt.REQUEST_REFUSED)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);

        Mockito.verify(timelineService).addTimelineElement(elementInternal, notification);
    }


    @Test
    void notificationRefusedSetNotificationCostRetry() {
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

        List<NotificationRefusedErrorInt> errors = Collections.singletonList(
                NotificationRefusedErrorInt.builder()
                        .errorCode("errorCode")
                        .detail("errorDetail")
                        .build()
        );
        Instant schedulingTime = Instant.now();

        final int recIndex = 0;
        final String iun = notification.getIun();
        UpdateNotificationCostResponseInt response = UpdateNotificationCostResponseInt.builder()
                .iun(iun)
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

        TimelineElementInternal elementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildRefusedRequestTimelineElement(notification, errors)).thenReturn(elementInternal);
        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        //WHEN
        Assertions.assertThrows(PnPaymentUpdateRetryException.class,
                () -> notificationRefusedActionHandler.notificationRefusedHandler(iun, errors, schedulingTime));

        
        //THEN
        ArgumentCaptor<List<PaymentsInfoForRecipientInt>> paymentForRecipientListCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(notificationProcessCostService).setNotificationStepCost(
                Mockito.eq(NotificationRefusedActionHandler.NOTIFICATION_REFUSED_COST),
                Mockito.eq(iun),
                paymentForRecipientListCaptor.capture(),
                Mockito.eq(schedulingTime),
                Mockito.eq(schedulingTime),
                Mockito.eq(UpdateCostPhaseInt.REQUEST_REFUSED)
        );

        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured = paymentForRecipientListCaptor.getValue();
        verifyPaymentInfo(notification, recIndex, paymentsInfoForRecipientsCaptured);

        Mockito.verify(timelineService, never()).addTimelineElement(elementInternal, notification);
    }


}