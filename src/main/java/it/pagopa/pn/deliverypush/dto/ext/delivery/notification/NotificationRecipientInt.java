package it.pagopa.pn.deliverypush.dto.ext.delivery.notification;



import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NotificationRecipientInt {
    private String taxId;
    private String internalId;
    private String denomination;
    private LegalDigitalAddressInt digitalDomicile;
    private PhysicalAddressInt physicalAddress;
    /* Aggiornato a nuovo oggetto pagamento
    private NotificationPaymentInfoInt payment;
     */
    private List<NotificationPaymentInfoIntV2> payments;
    private RecipientTypeInt recipientType;
}
