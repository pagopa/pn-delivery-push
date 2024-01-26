package it.pagopa.pn.deliverypush.dto.cost;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationProcessCost {
    private int partialCost;
    private Integer totalCost;
    private int analogCost;
    private int pagoPaBaseCost;
    private Integer vat;
    private Integer paFee;
    private Instant notificationViewDate;
    private Instant refinementDate;
}

