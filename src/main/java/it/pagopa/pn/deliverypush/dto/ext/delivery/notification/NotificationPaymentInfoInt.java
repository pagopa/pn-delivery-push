package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationPaymentInfoInt {
    private String noticeCode;
    private String creditorTaxId;

    private NotificationDocumentInt pagoPaForm;
    private NotificationDocumentInt f24flatRate;
    private NotificationDocumentInt f24standard;
}
