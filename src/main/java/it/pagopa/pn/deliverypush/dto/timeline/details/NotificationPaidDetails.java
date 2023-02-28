package it.pagopa.pn.deliverypush.dto.timeline.details;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaidDetails implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private String recipientType;
    private long amount;
    private String creditorTaxId;
    private String noticeCode;
    private String idF24;
    private String paymentSourceChannel;

    @Override
    public String toLog() {
        return this.toString(); // non ha info sensibili
    }
}
