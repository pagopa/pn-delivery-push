package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationpaid.NotificationPaidInt;

public class NotificationPaidMapper {

    private NotificationPaidMapper() {}

    public static NotificationPaidInt messageToInternal(PnDeliveryPaymentEvent.Payload paymentEvent, String idF24) {

        return NotificationPaidInt.builder()
                .iun(paymentEvent.getIun())
                .recipientIdx(paymentEvent.getRecipientIdx())
                .recipientType(NotificationPaidInt.RecipientTypeInt.valueOf(paymentEvent.getRecipientType().getValue()))
                .creditorTaxId(paymentEvent.getCreditorTaxId())
                .noticeCode(paymentEvent.getNoticeCode())
                .paymentDate(paymentEvent.getPaymentDate())
                .uncertainPaymentDate(paymentEvent.isUncertainPaymentDate())
                .paymentType(NotificationPaidInt.PaymentTypeInt.valueOf(paymentEvent.getPaymentType().getValue()))
                .amount(paymentEvent.getAmount())
                .paymentSourceChannel(paymentEvent.getPaymentSourceChannel())
                .idF24(idF24)
                .build();
    }
}
