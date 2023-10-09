package it.pagopa.pn.deliverypush.dto.ext.delivery.notificationpaid;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaidInt {

    private String iun;
    private int recipientIdx;
    private RecipientTypeInt recipientType;
    private PaymentTypeInt paymentType;
    private Instant paymentDate;
    private boolean uncertainPaymentDate;
    private String creditorTaxId;
    private String noticeCode;
    private String paymentSourceChannel;
    private Integer amount;

    @RequiredArgsConstructor
    @Getter
    @ToString
    public enum PaymentTypeInt {
        PAGOPA("PAGOPA");

        private final String value;

    }

    @RequiredArgsConstructor
    @Getter
    @ToString
    public enum RecipientTypeInt {
        PF("PF"),
        PG("PG");

        private final String value;

    }

}
