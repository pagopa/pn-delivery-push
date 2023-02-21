package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;



import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
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
    private PhysicalAddressInt physicalAddress;
    private NotificationPaymentInfoInt payment;
    private RecipientTypeInt recipientType;
}
