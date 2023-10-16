package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaInt;
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

}
