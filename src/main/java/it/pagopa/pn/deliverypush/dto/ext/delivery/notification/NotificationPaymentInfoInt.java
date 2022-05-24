package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaymentInfoInt {
    private NotificationDocumentInt pagoPaForm;
    private NotificationDocumentInt f24flatRate;
}
