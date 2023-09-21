package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaymentInfoIntV2 {
    private PagoPaInt pagoPA;
    private F24Int f24;
}
