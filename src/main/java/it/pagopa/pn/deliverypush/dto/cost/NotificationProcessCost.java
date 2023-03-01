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
    private int cost;
    private Instant notificationViewDate;
    private Instant refinementDate;
}

