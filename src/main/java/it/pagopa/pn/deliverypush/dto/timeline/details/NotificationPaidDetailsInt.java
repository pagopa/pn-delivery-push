package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaidDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private String recipientType;
    private Integer amount;
    private String creditorTaxId;
    private String noticeCode;
    private String idF24;
    private String paymentSourceChannel;
    private boolean uncertainPaymentDate;
    private Instant eventTimestamp;

    @Override
    public String toLog() {
        return this.toString(); // non ha info sensibili
    }
}
