package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;


import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationRecipientInt {
    private String taxId;
    private String internalId;
    private String denomination;
    private LegalDigitalAddressInt digitalDomicile;
    private PhysicalAddress physicalAddress;
    private NotificationPaymentInfoInt payment;
}
