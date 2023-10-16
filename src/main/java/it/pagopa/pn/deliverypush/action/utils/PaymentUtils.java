package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.cost.UpdateNotificationCostResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaInt;
import it.pagopa.pn.deliverypush.exceptions.PnPaymentUpdateRetryException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PaymentUtils {
    private PaymentUtils(){}
    
    @NotNull
    public static List<PaymentsInfoForRecipientInt> getPaymentsInfoFromNotification(NotificationInt notification) {
        List<PaymentsInfoForRecipientInt> paymentsInfoForRecipients = new ArrayList<>();

        notification.getRecipients().forEach(recipient -> {
            int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
            log.debug("Start add validation for recipient index {}", recIndex);

            recipient.getPayments().forEach( payment ->{
                final PagoPaInt pagoPaPayment = payment.getPagoPA();
                if(pagoPaPayment != null && Boolean.TRUE.equals(pagoPaPayment.getApplyCost())){
                    log.debug("Add validation for creditorTaxId={} noticeCode={} recIndex={}", pagoPaPayment.getCreditorTaxId(), pagoPaPayment.getNoticeCode(), recIndex);
                    PaymentsInfoForRecipientInt paymentsInfoForRecipient = PaymentsInfoForRecipientInt.builder()
                            .recIndex(recIndex)
                            .noticeCode(pagoPaPayment.getNoticeCode())
                            .creditorTaxId(pagoPaPayment.getCreditorTaxId())
                            .build();

                    paymentsInfoForRecipients.add(paymentsInfoForRecipient);
                }
            });
        });
        return paymentsInfoForRecipients;
    }

    public static void handleResponse(NotificationInt notification, UpdateNotificationCostResponseInt updateNotificationCostResponse) {
        log.debug("Start handle update cost response {}", updateNotificationCostResponse.getIun());

        updateNotificationCostResponse.getUpdateResults().forEach(response -> {
            PaymentsInfoForRecipientInt paymentsInfo = response.getPaymentsInfoForRecipient();

            log.debug("Start handle update response for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                    notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());

            switch (response.getResult()) {
                case OK -> log.debug("Update cost OK for iun={} recIndex={} creditorTaxId={} noticeCode={}",
                        notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                case KO -> log.error("Payment information is not valid. Can't update notification cost" +
                        " - creditorTaxId={} noticeCode={}", paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                case RETRY -> {
                    final String errorDetail = String.format("Update notification fee error, can't have response from service. iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRetryError(errorDetail);
                }
                default -> {
                    final String errorDetail = String.format("Update notification fee error. Response received is not handled for iun=%s recIndex=%s creditorTaxId=%s noticeCode=%s",
                            notification.getIun(), paymentsInfo.getRecIndex(), paymentsInfo.getCreditorTaxId(), paymentsInfo.getNoticeCode());
                    handleRetryError(errorDetail);
                }
            }
        });
    }

    private static void handleRetryError(String errorDetail) {
        log.info(errorDetail);
        throw new PnPaymentUpdateRetryException(errorDetail);
    }


}
