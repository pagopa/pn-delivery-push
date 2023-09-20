package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PagoPaInt {
    private String noticeCode;
    private String creditorTaxId;
    private Boolean applyCost;
    private NotificationDocumentInt attachment;
}
